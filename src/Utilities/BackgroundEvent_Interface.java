package Utilities;

/**
 * Represents an event in the background which would need to be polled for
 * and responded to.
 * @author johnmichaelreed2
 */
public interface BackgroundEvent_Interface {
    
    /**
     * Checks to see if a background event has occurred. If this method returns true, 
     * {@link #respondToEventOccurance() } is called to handle the event.
     * @return whether or not the event has occurred.
     */
    public boolean checkForEventOccurance();
    
    /**
     * This method is triggered when {@link #checkForEventOccurance() } returns true.
     * This method responds to the background event detected by {@link #checkForEventOccurance() }
     */
    public void respondToEventOccurance();
}
