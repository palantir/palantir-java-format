package com.palantir.javaformat.gradle;

import com.palantir.platform.OperatingSystem;
import javax.inject.Inject;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

public abstract class OperatingSystemValueSource implements ValueSource<OperatingSystem, ValueSourceParameters.None> {

    @Inject
    public OperatingSystemValueSource() {}

    @Override
    public OperatingSystem obtain() {
        return OperatingSystem.get();
    }
}
