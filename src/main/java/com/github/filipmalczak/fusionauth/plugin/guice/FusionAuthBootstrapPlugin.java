package com.github.filipmalczak.fusionauth.plugin.guice;

import com.google.inject.AbstractModule;
import io.fusionauth.plugin.spi.PluginModule;
import lombok.extern.slf4j.Slf4j;

@PluginModule
@Slf4j
public class FusionAuthBootstrapPlugin extends AbstractModule {
    @Override
    protected void configure() {
        log.info("Registering FusionAuth Bootstrap plugin");
        binder().bind(FusionAuthBootstrap.class).to(FusionAuthBootstrapImpl.class).asEagerSingleton();
    }
}
