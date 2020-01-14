/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal.deprecation;

import org.gradle.api.logging.configuration.WarningMode;
import org.gradle.internal.Factory;
import org.gradle.internal.featurelifecycle.DeprecatedFeatureUsage;
import org.gradle.internal.featurelifecycle.DeprecatedUsageBuildOperationProgressBroadcaster;
import org.gradle.internal.featurelifecycle.LoggingDeprecatedFeatureHandler;
import org.gradle.internal.featurelifecycle.UsageLocationReporter;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class DeprecationLogger {

    private static final ThreadLocal<Boolean> ENABLED = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };

    private static LoggingDeprecatedFeatureHandler deprecatedFeatureHandler = new LoggingDeprecatedFeatureHandler();

    public synchronized static void init(UsageLocationReporter reporter, WarningMode warningMode, DeprecatedUsageBuildOperationProgressBroadcaster buildOperationProgressBroadcaster) {
        deprecatedFeatureHandler.init(reporter, warningMode, buildOperationProgressBroadcaster);
    }

    public synchronized static void reset() {
        deprecatedFeatureHandler.reset();
    }

    public synchronized static void reportSuppressedDeprecations() {
        deprecatedFeatureHandler.reportSuppressedDeprecations();
    }

    @Nullable
    public static Throwable getDeprecationFailure() {
        return deprecatedFeatureHandler.getDeprecationFailure();
    }

    /**
     * Output format:
     * <p>
     * ${behaviour} This has been deprecated and is scheduled to be removed in Gradle {X}.
     **/
    public static void nagUserOfDeprecatedBehaviour(String behaviour) {
        if (isEnabled()) {
            nagUserWith(DeprecationMessage.behaviourHasBeenDeprecated(behaviour));
        }
    }

    // used in performance test - do not use in new code
    public static void nagUserOfDeprecated(String thing) {
        nagUserWith(DeprecationMessage.specificThingHasBeenDeprecated(thing));
    }

    public static void nagUserWith(DeprecationMessage.Builder deprecationMessageBuilder) {
        if (isEnabled()) {
            DeprecationMessage deprecationMessage = deprecationMessageBuilder.build();
            nagUserWith(deprecationMessage.toDeprecatedFeatureUsage(DeprecationLogger.class));
        }
    }

   /* private static void nagUserWith(DeprecationBuilder builder) {
        if (isEnabled()) {
            DeprecationMessage deprecationMessage = builder.build();
            nagUserWith(deprecationMessage.toDeprecatedFeatureUsage(DeprecationLogger.class));
        }
    }*/

    @Nullable
    public static <T> T whileDisabled(Factory<T> factory) {
        ENABLED.set(false);
        try {
            return factory.create();
        } finally {
            ENABLED.set(true);
        }
    }

    public static void whileDisabled(Runnable action) {
        ENABLED.set(false);
        try {
            action.run();
        } finally {
            ENABLED.set(true);
        }
    }

    private static boolean isEnabled() {
        return ENABLED.get();
    }

    private synchronized static void nagUserWith(DeprecatedFeatureUsage usage) {
        deprecatedFeatureHandler.featureUsed(usage);
    }
}
