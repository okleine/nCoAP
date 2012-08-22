package de.uniluebeck.itm.spitfire.nCoap.message.options;

import com.google.common.collect.LinkedListMultimap;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionOccurence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author Oliver Kleine
 */
public class OptionList {

    private static Logger log = LoggerFactory.getLogger(OptionList.class.getName());
    public static int MAX_NUMBER_OF_OPTIONS = 15;


    //list of options to be included
    private LinkedListMultimap<OptionName, Option> options;

    /**
     * Creates a new empty {@link OptionList}. The paramter {@link Code} is to ensure that only meaningful options
     * are included in the list. If an {@link Option} is meaningful or not depends on the {@link Code}.
     */
    public OptionList(){
        options = LinkedListMultimap.create(0);
    }

    /**
     * This method is to add a new {@link Option} to the {@link OptionList}. The Option will only be added if it
     * satisfies some constraints. It must be meaningful with the {@link Code}, given as method parameter.
     * If the OptionList already contains an Option with the same type (i.e. {@link OptionName}),
     * the new Option will be added if and only if it may occur multiple times in a message with the abovementioned
     * code.
     *
     * @param code the message code
     * @param optionName the name of the option to be added
     * @param option The option to be added
     * @throws InvalidOptionException if the option cannot be added but is critical and is either of an
     * unrecognized type (see {@link OptionRegistry.OptionName}) or occured more often than allowed
     * @throws ToManyOptionsException if adding the option to the list would exceed the maximum number of
     * options per message
     */
    public void addOption(Code code, OptionName optionName, Option option)
            throws InvalidOptionException, ToManyOptionsException {

        OptionOccurence allowed_occurence = OptionRegistry.getAllowedOccurence(code, optionName);

        if(allowed_occurence == OptionRegistry.OptionOccurence.NONE){
            String msg = "[OptionList] " + optionName + " option has no meaning with"
                            + " a message with code " + code + ".";
            throw new InvalidOptionException(option.getOptionNumber(), msg);
        }
        else if(allowed_occurence == OptionRegistry.OptionOccurence.ONCE){
            if(options.containsKey(optionName)){
                String msg = "[OptionList] " + optionName + " option may not occur multiple times"
                                + " in a message with code " + code + ".";
                if(optionName == OptionName.URI_HOST){
                    StringOption host = (StringOption) (options.get(OptionName.URI_HOST).get(0));
                    msg = msg + " Current value: " + host.getDecodedValue();

                }
                throw new InvalidOptionException(option.getOptionNumber(), msg);
            }
        }

        //Check if adding the option would exceed the maximum list size
        if(options.size() >= MAX_NUMBER_OF_OPTIONS){
            String msg = "[OptionList] There are already " + MAX_NUMBER_OF_OPTIONS + " options contained.";
            throw new ToManyOptionsException(msg);
        }

        //Option is meaningful and fulfills constraints, so add it to the list
        if(!options.put(optionName, option)){
            String msg = "[OptionList] " + optionName + " with equal value is already contained in the message.";
            throw new InvalidOptionException(option.getOptionNumber(), msg);
        }

        log.debug("" + optionName + " option with value " + Option.getHexString(option.getValue()) +
                " succesfully added to option list.");
    }

    /**
     * Returns a set of matching options from the list in order of there adding to the list
     * @param optionName the name of the requested options
     * @return a set of matching options from the list in order of there adding to the list
     */
    public List<Option> getOption(OptionName optionName){
        return options.get(optionName);
    }

    /**
     * Removes all options of the given OptionName from the option list and returns the number of removed options
     * @param optionName The name of the options to be removed from the option list
     * @return The number of removed options
     */
    public int removeAllOptions(OptionName optionName){
        return options.removeAll(optionName).size();
    }

    /**
     * Removes all target URI specific options from the list
     *
     * @return the number of removed options
     */
    public int removeTargetURI() {
        int deleted = 0;
        //Delete all target URI specific options and add the number of deleted options to the result value
        deleted += options.removeAll(OptionName.URI_HOST).size();
        deleted += options.removeAll(OptionName.URI_PATH).size();
        deleted += options.removeAll(OptionName.URI_PORT).size();
        deleted += options.removeAll(OptionName.URI_QUERY).size();

        log.debug("Removed " + deleted + " target URI options from option list");
        return deleted;
    }

    /**
     * Removes all location URI related options from the list
     *
     * @return the number of removed options
     */
    public int removeLocationURI(){
        int deleted = 0;
        deleted += options.removeAll(OptionName.LOCATION_PATH).size();
        deleted += options.removeAll(OptionName.LOCATION_QUERY).size();
        return deleted;
    }

    public int getOptionCount(){
        return options.size();
    }

    /**
     * Two instances of {@link OptionList} are equal if and only if both contain the same number of {@link Option}s
     * of each {@link OptionName} in the same order per {@link OptionName}. To be more precisely, internally the
     * {@link Option}s are stored in a {@link LinkedListMultimap} with {@link OptionName} as key and an instance of
     * {@link Option} as value. The order of the keys does not influence the equality of two {@link OptionList}s
     * but the order of the values per key does.
     *
     * @param obj The object to test the equality with
     * @return  <code>true</code> if the given object is an instance of {@link OptionList} and is equal to this instance
     * according to the conditions explained above. Otherwise it returns <code>false</code>.
     */
    public boolean equals(Object obj){
        if(!(obj instanceof OptionList)){
            return false;
        }
        
        OptionList optionList = (OptionList) obj;
        
        if(options.size() != optionList.options.size()){
            return false;
        }
        
        for(OptionName optionName : OptionName.values()){
            List<Option> list1 = options.get(optionName);
            List<Option> list2 = optionList.options.get(optionName);
            
            if(list1.size() != list2.size()){
                return false;
            }
            
            for(int i = 0; i < list1.size(); i++ ){
                if(!list1.get(i).equals(list2.get(i))){
                    return false;
                }
            }
        }

        return true;
    }
}