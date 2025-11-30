package com.redhat.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "banking")
public interface BankingConfig {

    @WithName("title")
    String title();
}
