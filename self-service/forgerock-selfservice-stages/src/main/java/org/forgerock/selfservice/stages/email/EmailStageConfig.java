package org.forgerock.selfservice.stages.email;

import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.StageType;

/**
 * Configuration for the email stage.
 *
 * @since 0.1.0
 */
public class EmailStageConfig implements StageConfig {

    public static final StageType<EmailStageConfig> TYPE =
            StageType.valueOf("emailValidation", EmailStageConfig.class);

    @Override
    public StageType<?> getStageType() {
        return TYPE;
    }

}
