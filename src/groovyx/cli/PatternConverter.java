package groovyx.cli;

import com.beust.jcommander.IStringConverter;

import java.util.regex.Pattern;

/**
 * Created by jim on 5/10/14.
 */
public class PatternConverter implements IStringConverter<Pattern> {
    @Override
    public Pattern convert(String s) { return Pattern.compile(s); }
}
