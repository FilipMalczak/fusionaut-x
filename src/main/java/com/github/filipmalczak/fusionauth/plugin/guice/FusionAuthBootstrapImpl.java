package com.github.filipmalczak.fusionauth.plugin.guice;

import com.google.inject.Inject;
import com.inversoft.authentication.api.domain.AuthenticationKey;
import com.inversoft.authentication.api.domain.AuthenticationKeyMapper;
import io.fusionauth.api.domain.ApplicationMapper;
import io.fusionauth.api.domain.UserMapper;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.ContentStatus;
import io.fusionauth.domain.User;
import io.fusionauth.domain.UserRegistration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Slf4j
public class FusionAuthBootstrapImpl implements FusionAuthBootstrap {
    private static final String BOOTSTRAPPED_KEY_ATTRIBUTE = "fusionauth.bootstrap";

    private static final String ENVVAR_PREFIX = "FUSIONAUTH_BOOTSTRAP_";
    private static final String API_KEY_ENVVAR = ENVVAR_PREFIX+"API_KEY";
    private static final String EXISTING_KEY_ENVVAR = ENVVAR_PREFIX+"EXISTING_KEY_STRATEGY";
    private static final String IGNORE_API_KEY_MISSING_ENVVAR = ENVVAR_PREFIX+"IGNORE_API_KEY_MISSING";

    private static final String ADMIN_USERNAME_ENVVAR = ENVVAR_PREFIX+"ADMIN_USERNAME";
    private static final String ADMIN_PASSWORD_ENVVAR = ENVVAR_PREFIX+"ADMIN_PASSWORD";
    private static final String ADMIN_EMAIL_ENVVAR = ENVVAR_PREFIX+"ADMIN_EMAIL";
    private static final String ADMIN_FIRSTNAME_ENVVAR = ENVVAR_PREFIX+"ADMIN_FIRSTNAME";
    private static final String ADMIN_LASTNAME_ENVVAR = ENVVAR_PREFIX+"ADMIN_LASTNAME";

    private static final String EXISTING_ADMIN_ENVVAR = ENVVAR_PREFIX+"EXISTING_ADMIN_STRATEGY";
    private static final String IGNORE_ADMIN_MISSING_ENVVAR = ENVVAR_PREFIX+"IGNORE_ADMIN_MISSING";

    @Inject
    public FusionAuthBootstrapImpl(AuthenticationKeyMapper authenticationKeyMapper, UserMapper userMapper, ApplicationMapper applicationMapper){
        try {
            bootstrapApiKey(authenticationKeyMapper);
            bootstrapAdmin(userMapper, applicationMapper);
        } catch (Throwable t){
            shutdown("Something went all the way wrong while trying to bootstrap FusionAuth. Shutting down.", t);
        }
    }

    private void bootstrapAdmin(UserMapper userMapper, ApplicationMapper applicationMapper){
        AdminDetails adminDetails = new AdminDetails();
        List<String> missingEnvvars = adminDetails.getMissingEnvvars();
        if (missingEnvvars.isEmpty()) {
            log.info("Making sure that admin is bootstrapped");
            Application application = applicationMapper.retrieveById((UUID)null, Application.FUSIONAUTH_APP_ID);
            manageAdmins(userMapper, adminDetails, application);
        } else {
            String ignoreFailure = System.getenv(IGNORE_ADMIN_MISSING_ENVVAR);
            if (ignoreFailure == null || !ignoreFailure.equals("true")){
                log.error(
                    "'Ignore missing admin details' option is turned off! Failing fast. " +
                        "To allow no admin boostrapping, set envvar "+ IGNORE_ADMIN_MISSING_ENVVAR +" to 'true'!"
                );
                shutdown(
                    "Following envvars are required for admin bootstrapping, yet missing: "+
                        String.join(", ", missingEnvvars)+
                        "!"
                );
            }
        }
    }

    private void manageAdmins(UserMapper mapper, AdminDetails adminDetails, Application application){
        List<User> existing = getBootstrappedUsers(mapper);
        if (existing.isEmpty()){
            log.info("Admin account missing, bootstraping!");
            add(mapper, adminDetails, application);
            log.info("Admin account successfully bootstrapped");
        } else {
            log.info("An admin account is already bootstrapped");
            ExistingAdminStrategy strategy = getExistingAdminStrategy();
            log.info("Using following strategy to handle existing admin account: "+strategy);
            switch (strategy) {
                case FAIL: failOnAdminAccounts(existing); break;
                case REPLACE: replace(mapper, existing, adminDetails, application); break;
                case ADD: add(mapper, adminDetails, application); break;
                default: shutdown("This should never happen! Please replace the code to handle strategy "+strategy+"!");
            }
            log.info("Admin account successfully bootstrapped");
        }
    }

    private static class AdminDetails {
        String username;
        String password;
        String email;
        String firstName;
        String lastName;

        AdminDetails() {
            this.username = System.getenv(ADMIN_USERNAME_ENVVAR);
            this.password = System.getenv(ADMIN_PASSWORD_ENVVAR);
            this.email = System.getenv(ADMIN_EMAIL_ENVVAR);
            this.firstName = System.getenv(ADMIN_FIRSTNAME_ENVVAR);
            this.lastName = System.getenv(ADMIN_LASTNAME_ENVVAR);
        }

        List<String> getMissingEnvvars(){
            List<String> result = new LinkedList<>();
            if (username == null)
                result.add(ADMIN_USERNAME_ENVVAR);
            if (password == null)
                result.add(ADMIN_PASSWORD_ENVVAR);
            if (email == null)
                result.add(ADMIN_EMAIL_ENVVAR);
            return result;
        }
    }

    private ExistingAdminStrategy getExistingAdminStrategy(){
        String envvar = System.getenv(EXISTING_ADMIN_ENVVAR);
        if (envvar == null){
            log.info("No "+EXISTING_ADMIN_ENVVAR+" found; falling back to "+ExistingAdminStrategy.DEFAULT);
            return ExistingAdminStrategy.DEFAULT;
        }
        try {
            return ExistingAdminStrategy.valueOf(envvar);
        } catch (IllegalArgumentException e){
            shutdown("Cannot find existing admin strategy "+envvar, e);
            return null; //needed because compiler doesnt know that shutdown will cause System.exit
        }
    }


    private List<User> getBootstrappedUsers(UserMapper userMapper){
        List<User> result = new LinkedList<>();
        List<User> currentBatch;
        int offset = 0;
        int limit = 50;
        do {
            currentBatch = userMapper.retrieveLimitOffset(limit, offset);
            offset += limit;
            result.addAll(
                currentBatch
                    .stream()
                    .filter(u -> u.tenantId == null)
                    .filter(u ->
                        u.getRegistrations()
                            .stream()
                            .anyMatch(r ->
                                r.applicationId.equals(Application.FUSIONAUTH_APP_ID)
                            )
                    )
                    .filter(this::isBootstrapped)
                    .collect(toList())
            );
        } while (!currentBatch.isEmpty());
        return result;
    }

    private boolean isBootstrapped(User user){
        try {
            return ("" + true).equals(user.data.get(BOOTSTRAPPED_KEY_ATTRIBUTE));
        } catch (NullPointerException e){
            return false;
        }
    }

    private void add(UserMapper userMapper, AdminDetails adminDetails, Application application){
        log.info("Creating new admin account");
        userMapper.create(
            new User(
                null,
                adminDetails.email, adminDetails.username, adminDetails.password,
                null, null, null,
                adminDetails.firstName, null, adminDetails.lastName,
                null, null,
                true, null, null,
                Collections.singletonMap(BOOTSTRAPPED_KEY_ATTRIBUTE, true),
                true,
                ContentStatus.ACTIVE,
                null, false, null, null,
                new UserRegistration(
                    null,
                    application.id,
                    null, null,
                    adminDetails.username, ContentStatus.ACTIVE,
                    null, null, null,
                    "admin"
                )
            )
        );
    }

    private void replace(UserMapper userMapper, List<User> existing, AdminDetails adminDetails, Application application){
        log.info("Removing "+existing.size()+" admin accounts");
        existing.stream().map(u -> u.id).forEach(userMapper::delete);
        add(userMapper, adminDetails, application);

    }

    private void bootstrapApiKey(AuthenticationKeyMapper authenticationKeyMapper){
        String apiKey = System.getenv(API_KEY_ENVVAR);
        if (apiKey != null){
            log.info("Making sure that API key is bootstrapped");
            manageKeys(authenticationKeyMapper, apiKey);
        } else {
            String ignoreFailure = System.getenv(IGNORE_API_KEY_MISSING_ENVVAR);
            if (ignoreFailure == null || !ignoreFailure.equals("true")){
                log.error(
                    "'Ignore missing API key' option is turned off! Failing fast. " +
                        "To allow no API key boostrapping, set envvar "+ IGNORE_API_KEY_MISSING_ENVVAR +" to 'true'!"
                );
                shutdown("No "+API_KEY_ENVVAR+" envvar found!");
            }
        }
    }

    private void manageKeys(AuthenticationKeyMapper mapper, String requiredKey){
        List<AuthenticationKey> existing = getBootstrappedKeys(mapper);
        if (existing.isEmpty()){
            log.info("API key missing, bootstraping!");
            add(mapper, requiredKey);
            log.info("API key successfully bootstrapped");
        } else {
            if (existing.stream().anyMatch(k -> k.id.equals(requiredKey))) {
                log.info("API key already bootstrapped with required value; no actions required");
            } else {
                log.info("API key already bootstrapped, but its value is different than required");
                ExistingKeyStrategy strategy = getExistingKeyStrategy();
                log.info("Using following strategy to handle existing bootstrapped keys: "+strategy);
                switch (strategy) {
                    case FAIL: failOnApiKey(existing); break;
                    case UPDATE: replace(mapper, existing, requiredKey, false); break;
                    case REPLACE: replace(mapper, existing, requiredKey, true); break;
                    case ADD: add(mapper, requiredKey); break;
                    default: shutdown("This should never happen! Please replace the code to handle strategy "+strategy+"!");
                }
                log.info("API key successfully bootstrapped");
            }
        }
    }

    private List<AuthenticationKey> getBootstrappedKeys(AuthenticationKeyMapper mapper){
        return mapper.retrieveAll().stream().filter(this::isBootstrapped).collect(toList());
    }


    private void shutdown(String message){
        shutdown(message, 1, null);
    }

    private void shutdown(String message, Throwable t){
        shutdown(message, 1, t);
    }

    private void shutdown(String message, int code, Throwable t){
        log.error(message);
        if (t != null){
            t.printStackTrace();
        }
        Executors.newSingleThreadExecutor().submit(()->doShutdown(code));

    }

    @SneakyThrows
    private void doShutdown(int code){
        log.error("Scheduled hard system shutdown in 5s");
        Thread.sleep(5000); //just to let Tomcat flush the streams, etc
        System.exit(code);
    }

    private void failOnApiKey(List<AuthenticationKey> existing){
        shutdown("There are "+existing.size()+" bootstrapped API keys, but none has required value");
    }

    private void failOnAdminAccounts(List<User> existing){
        shutdown("There are "+existing.size()+" bootstrapped admin accounts");
    }

    private void replace(AuthenticationKeyMapper mapper, List<AuthenticationKey> existing, String required, boolean replaceAll){
        if (existing.size() != 1 && !replaceAll){
            failOnApiKey(existing);

        }
        log.info("Removing "+existing.size()+" bootstrapped keys");
        existing.stream().map(k -> k.id).forEach(mapper::delete);
        add(mapper, required);

    }

    private void add(AuthenticationKeyMapper mapper, String required){
        log.info("Creating new API key");
        mapper.create(constructBootstrappedKey(required));
    }

    private ExistingKeyStrategy getExistingKeyStrategy(){
        String envvar = System.getenv(EXISTING_KEY_ENVVAR);
        if (envvar == null){
            log.info("No "+EXISTING_KEY_ENVVAR+" found; falling back to "+ExistingKeyStrategy.DEFAULT);
            return ExistingKeyStrategy.DEFAULT;
        }
        try {
            return ExistingKeyStrategy.valueOf(envvar);
        } catch (IllegalArgumentException e){
            shutdown("Cannot find existing API key strategy "+envvar, e);
            return null; //needed because compiler doesnt know that shutdown will cause System.exit
        }
    }

    private boolean isBootstrapped(AuthenticationKey authenticationKey){
        try {
            return authenticationKey.metaData.attributes.get(BOOTSTRAPPED_KEY_ATTRIBUTE).toLowerCase().equals("" + true);
        } catch (NullPointerException e){
            return false;
        }
    }

    private AuthenticationKey constructBootstrappedKey(String apiKey){
        return new AuthenticationKey(apiKey, new AuthenticationKey.AuthenticationPermissions(), new AuthenticationKey.AuthenticationMetaData(BOOTSTRAPPED_KEY_ATTRIBUTE, ""+true));
    }

    enum ExistingKeyStrategy {
        /**
         * If there is one bootstrapped key, replace it (delete and insert new); if there is more than one - failOnApiKey.
         */
        UPDATE,
        /**
         * Delete all the existing bootstrapped keys and create a new one.
         */
        REPLACE,
        /**
         * Ignore existing keys, just make sure that required value is present by adding a new one.
         */
        ADD,
        /**
         * Guess yourself.
         */
        FAIL;

        public static final ExistingKeyStrategy DEFAULT = REPLACE;
    }

    enum ExistingAdminStrategy {
        /**
         * Delete all the existing admins keys and create a new one.
         */
        REPLACE,
        /**
         * Ignore existing admins, just make sure that there is an admin with matching login
         */
        ADD,
        /**
         * Guess yourself.
         */
        FAIL;

        public static final ExistingAdminStrategy DEFAULT = REPLACE;
    }
}
