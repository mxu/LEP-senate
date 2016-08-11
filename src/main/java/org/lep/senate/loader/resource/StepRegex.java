package org.lep.senate.loader.resource;

import com.beust.jcommander.JCommander;
import org.lep.senate.model.Step;
import org.lep.settings.CongressSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StepRegex {
    private static final Logger logger = LoggerFactory.getLogger(StepRegex.class);

    private static final String STEP_REGEX_FORMAT = "%s.txt";

    private static Map<Step, List<Pattern>> stepRegexMap = new HashMap<>();

    public static void main(String[] args) {
        CongressSettings settings = new CongressSettings();
        new JCommander(settings, args);

        Step step = settings.getStep();
        if(step == null) {
            for(Step s : Step.values()) {
                printRegexList(s);
            }
        } else {
            printRegexList(step);
        }
    }

    private static void printRegexList(Step step) {
        try {
            List<Pattern> regexList = getRegexList(step);
            logger.info("{} regexes for {}:", regexList.size(), step.name());
            regexList.forEach(regex -> logger.info(regex.pattern()));
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<Pattern> getRegexList(Step step) throws FileNotFoundException {
        if(!stepRegexMap.containsKey(step)) {
            List<String> lines = ResourceLoader.asList(String.format(STEP_REGEX_FORMAT, step.name()));

            stepRegexMap.put(step,
                    lines.stream()
                            .map(line -> Pattern.compile(line.length() > 6 ? line.substring(1, line.length() - 2) : ""))
                            .collect(Collectors.toList()));
        }

        return stepRegexMap.get(step);
    }

    public static boolean matchesStepRegex(Step step, String action) throws FileNotFoundException {
        for(Pattern p : getRegexList(step)) {
           if(p.matcher(action).find()) {
               logger.debug(String.format("\"%s\" matches %s regex \"%s\"", action, step.name(), p.pattern()));
               return true;
           }
        }

        return false;
    }
}
