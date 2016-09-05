/**
 * Created by User on 05.09.2016.
 */
public enum Indicator
{
    /**
     * Object is complete with no issues
     */
    SUCCESSFUL,
    /**
     * Object data has to be updated in next program run because it is not representative
     */
    EARLY_DATA,
    /**
     * Timeout reached while attempting to get article from web
     */
    TIMEOUT,
    /**
     * Query didn't reach appropriate element when processed article
     */
    SELECTOR_ERROR,
    /**
     * Date, time or author couldn't be parsed, format was wrong
     */
    PARSE_ERROR
}
