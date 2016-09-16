
public enum Indicator
{
    /**
     * Object is complete with no issues
     */
    SUCCESS,
    /**
     * Object data has to be updated in next program run because it is not representative
     */
    EARLY_DATA, //has to be represented!
    /**
     * Timeout reached while attempting to get article from web
     */
    TIMEOUT,
    /**
     * Query didn't reach appropriate element when processed article
     */
    SELECTOR_ERROR,
    /**
     * Date, time or author couldn't be parsed, data format was wrong
     */
    PARSE_ERROR,
    /**
     * When no match with date or author pattern found
     */
    REGEX_PATTERN_ERROR,
    /**
     * Work on this article in next versions of program
     */
    SUSPENDED,
    /**
     * In cases of IOEXCEPTION
     */
    IOEXCEPTION,
    /**
     * For example if URL is wrong
     */
    BAD_INPUT
}
