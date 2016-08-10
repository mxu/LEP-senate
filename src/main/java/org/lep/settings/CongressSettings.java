package org.lep.settings;

import com.beust.jcommander.Parameter;
import org.lep.senate.model.Step;

public class CongressSettings {
    @Parameter(names="-c",
               description="Congress number")
    private Integer congress;

    @Parameter(names="-b",
               description="Bill number")
    private Integer bill;

    @Parameter(names="-s",
               description="Legislative step",
               validateWith=StepValidator.class)
    private Step step;

    public Integer getCongress() { return congress; }
    public Integer getBill() { return bill; }
    public Step getStep() { return step; }
}
