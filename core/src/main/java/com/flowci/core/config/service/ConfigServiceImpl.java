package com.flowci.core.config.service;

import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.dao.ConfigDao;
import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.ConfigParser;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.domain.SmtpOption;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Service
public class ConfigServiceImpl implements ConfigService {

    @Value("classpath:default/smtp-demo-config.yml")
    private Resource defaultSmtpConfigYml;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private SpringEventManager eventManager;

    @EventListener
    public void onInit(ContextRefreshedEvent ignore) {
        try {
            Config config = ConfigParser.parse(defaultSmtpConfigYml.getInputStream());
            Optional<Config> optional = configDao.findByName(config.getName());

            // delete if default smtp config is disabled
            if (!appProperties.isDefaultSmtpConfig()) {
                optional.ifPresent(value -> configDao.delete(value));
                return;
            }

            if (optional.isPresent()) {
                return;
            }
            configDao.save(config);
            log.info("Config {} has been created", config.getName());
        } catch (IOException e) {
            log.warn(e);
        }
    }

    @Override
    public Config get(String name) {
        Optional<Config> optional = configDao.findByName(name);
        if (!optional.isPresent()) {
            throw new NotFoundException("Configuration name {0} is not found", name);
        }
        return optional.get();
    }

    @Override
    public List<Config> list() {
        return configDao.findAll(Mongoable.SortByCreatedAtASC);
    }

    @Override
    public List<Config> list(Config.Category category) {
        return configDao.findAllByCategoryOrderByCreatedAtAsc(category);
    }

    @Override
    public Config save(String name, SmtpOption option) {
        Objects.requireNonNull(name, "Config name is required");

        SmtpConfig config;

        Optional<Config> optional = configDao.findByName(name);
        if (optional.isPresent()) {
            config = (SmtpConfig) optional.get();
        } else {
            config = new SmtpConfig();
            config.setName(name);
        }

        config.setSmtp(option);

        if (config.hasSecret()) {
            setAuthFromSecret(config);
        }

        try {
            return configDao.save(config);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Config name {0} is already defined", config.getName());
        }
    }

    private void setAuthFromSecret(SmtpConfig config) {
        GetSecretEvent event = eventManager.publish(new GetSecretEvent(this, config.getSecret()));
        if (event.hasError()) {
            throw event.getError();
        }

        Secret secret = event.getFetched();
        if (secret.getCategory() != Secret.Category.AUTH) {
            throw new ArgumentException("Invalid secret type");
        }

        config.setAuth((SimpleAuthPair) secret.toSimpleSecret());
    }

    @Override
    public Config delete(String name) {
        Config config = get(name);
        configDao.deleteById(config.getId());
        return config;
    }
}
