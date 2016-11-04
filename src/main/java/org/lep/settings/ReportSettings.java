package org.lep.settings;

import com.beust.jcommander.Parameter;

public class ReportSettings {
    @Parameter(names="-c",
               description="Congress number")
    private Integer congress;

    public Integer getCongress() { return congress; }
}
