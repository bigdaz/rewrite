/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.semver;

import lombok.Value;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;

@Value
public class ExactVersion implements VersionComparator {
    String version;

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        return this.version.equals(version);
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        return 0;
    }

    public static Validated build(String pattern) {
        String versionOnly;
        int hyphenIndex = pattern.indexOf('-');
        if(hyphenIndex == -1) {
            versionOnly = pattern;
        } else {
            versionOnly = pattern.substring(0, hyphenIndex);
        }
        if(versionOnly.startsWith("latest") ||
                versionOnly.contains("x") ||
                versionOnly.contains("^") ||
                versionOnly.contains("~") ||
                versionOnly.contains(" ")) {
            return Validated.invalid("exactVersion", pattern, "not an exact version number");
        }
        return Validated.valid("exactVersion", new ExactVersion(pattern));
    }
}
