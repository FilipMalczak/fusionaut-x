package com.github.filipmalczak.fusionauth.plugin.guice;

/**
 * Dummy interface, its implementation should be bound as an eager singleton
 * and its constructor should perform FA bootstrapping (initializing API key,
 * creating adming user, etc).
 */
public interface FusionAuthBootstrap {
}
