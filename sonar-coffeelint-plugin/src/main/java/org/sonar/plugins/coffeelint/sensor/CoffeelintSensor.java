package org.sonar.plugins.coffeelint.sensor;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.coffeelint.language.Coffee;
import org.sonar.plugins.coffeelint.model.CoffeelintViolation;
import org.sonar.plugins.coffeelint.sensor.CoffeelintAnalyser.CoffeeOutputHandler;

/**
 * {@link Sensor} to analyse <i>coffee</i> files via http://www.coffeelint.org
 *
 * @author andreas
 * @author spartan2015
 */
public class CoffeelintSensor implements Sensor {
    private final FileSystem fileSystem;
    private final FilePredicate mainFilePredicate;
    private final ResourcePerspectives resourcePerspectives;
    protected SensorContext context;

    public CoffeelintSensor(FileSystem fileSystem, ResourcePerspectives resourcePerspectives) {
        this.fileSystem = fileSystem;
        this.resourcePerspectives = resourcePerspectives;
        this.mainFilePredicate = fileSystem.predicates().and(fileSystem.predicates().hasType(InputFile.Type.MAIN),
                fileSystem.predicates().hasLanguage(Coffee.KEY));
    }

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.name("CoffeeLint");
    }

    @Override
    public void execute(org.sonar.api.batch.sensor.SensorContext sensorContext) {
        this.context = sensorContext;
        if (shouldExecuteOnProject()) {
            fileSystem.inputFiles(mainFilePredicate).forEach(inputFile -> checkCoffeeFile(inputFile));
        }
    }

    private boolean shouldExecuteOnProject() {
        return fileSystem.hasFiles(mainFilePredicate);
    }

    private void checkCoffeeFile(InputFile file) {
        CoffeeOutputHandler outputHandler = new CoffeeOutputHandler(file);
        new CoffeelintAnalyser().analyse(outputHandler, file.file());
        processIssues(file, outputHandler.getViolation());
    }

    private void processIssues(InputFile file, CoffeelintViolation violation) {
        if (violation.getViolations() != null) {
            violation.getViolations().stream().forEach(v -> {
                saveIssue(file, v.getLineNumber(), v.getRule(), v.getMessage());
            });
        }
    }

    private void saveIssue(final InputFile inputFile, int line, final String externalRuleKey, final String message) {
        RuleKey ruleKey = RuleKey.of(Coffee.KEY, externalRuleKey);

        NewIssue newIssue = context.newIssue()
                .forRule(ruleKey);

        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(inputFile)
                .message(message);
        if (line > 0) {
            primaryLocation.at(inputFile.selectLine(line));
        }
        newIssue.at(primaryLocation);

        newIssue.save();
    }
}
