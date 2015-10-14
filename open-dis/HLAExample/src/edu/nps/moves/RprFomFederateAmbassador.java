/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.nps.moves;

import hla.rti.jlc.NullFederateAmbassador;

/**
 *
 * @author DMcG
 */
public class RprFomFederateAmbassador extends NullFederateAmbassador
{
    /** the time of this federate **/
    protected double federateTime        = 0.0;
    /** The minimum amount of time ahead of federate time this federate can schedule a future event. The
     * federate cannot publish events sooner than currentTime + lookahead
     **/ 
    protected double federateLookahead   = 1.0;

    /** Regulating means that this federate issues timestamped events--thus causing other federates
     *  to not advance their clocks because we may issue events that they must process */
    protected boolean isRegulating       = false;
    
    /** Constrained means that this federate consumes timestamped events; thus it cannot advance it's
     * clock time without considering the time state of federates that issue time stamped events
     */
    protected boolean isConstrained      = false;
    
    /** The federate is in "time-advancing" state, that is it is receiving time-stamped
     * messages. 
     * */
    protected boolean isAdvancing        = false;

    /** 
     * */
    protected boolean isAnnounced        = false;
    
    /**
     * 
     */
    protected boolean isReadyToRun       = false;
    
    private void log( String message )
	{
		System.out.println( "FederateAmbassador: " + message );
	}
	
	//////////////////////////////////////////////////////////////////////////
	////////////////////////// RTI Callback Methods //////////////////////////
	//////////////////////////////////////////////////////////////////////////
	public void synchronizationPointRegistrationFailed( String label )
	{
		log( "Failed to register sync point: " + label );
	}

	public void synchronizationPointRegistrationSucceeded( String label )
	{
		log( "Successfully registered sync point: " + label );
	}

	public void announceSynchronizationPoint( String label, byte[] tag )
	{
		log( "Synchronization point announced: " + label );
		if( label.equals(RprFomFederate.RUN_SYNC_POINT) )
			this.isAnnounced = true;
	}

	public void federationSynchronized( String label )
	{
		log( "Federation Synchronized: " + label );
		if( label.equals(RprFomFederate.RUN_SYNC_POINT) )
			this.isReadyToRun = true;
	}

}
