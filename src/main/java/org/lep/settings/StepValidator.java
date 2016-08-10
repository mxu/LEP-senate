package org.lep.settings;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.lep.senate.model.Step;

public class StepValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        try {
            Step.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Parameter " + name + " must be a valid legislative step");
        }
    }
}
