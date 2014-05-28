package src.com.key;

/**
 *  KeyAlreadyExistsException represents the exception when a public key for
 *  the same phone number is attempted to be set more than once.
 *
 *  This class has no constructor because the offending phone number should
 *  be quite obvious.
 */
public class KeyAlreadyExistsException extends Exception
{
}