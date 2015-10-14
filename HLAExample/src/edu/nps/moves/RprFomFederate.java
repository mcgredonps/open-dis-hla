
package edu.nps.moves;

import java.io.*;
import java.net.MalformedURLException;

import hla.rti.AttributeHandleSet;
import hla.rti.FederatesCurrentlyJoined;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.FederationExecutionDoesNotExist;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.ResignAction;
import hla.rti.SuppliedAttributes;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.lrc.compat.JFederationExecutionAlreadyExists;


import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

/**
 * The RrpFomFederate is an example federate that uses the RPR-FOM.
 * 
 * @author DMcG
 */
public class RprFomFederate 
{
    public static final String RUN_SYNC_POINT = "SimulationStartRendevousPoint";
    public static final String FEDERATION_NAME = "RPRFOMFederation";
    
    /** site ID, part of the (site, application, entity) triplet identifier */
    private short SITE_ID = 1;
    
    /** Application ID, part of the (site, application, entity) triplet identifer */
    private short APPLICATION_ID = 1;
    
    /** Entity ID, part of the triplet for entity identifiers. 0 is not a valid eid. */
    private short entityID = (short) ( ( (Short.MAX_VALUE - 1) * Math.random() ) + 1);
                              
    // Cartesian (using WGS84) location for monterey at 36.6, -121.9. See
    // http://www.apsalin.com/convert-geodetic-to-cartesian.aspx
    private double[] montereyLocation = {-2709083.22564985, -4352324.21057452, 3781849.12057825};

    
    /** The RTIambassador is how we communicate with the rti */
    private RTIambassador rtiAmbassador;
    
    /** The federate ambassador is how the RTI communicates with us */
    private RprFomFederateAmbassador federateAmbassador;
    
    /** Handle for the physical entity class in RPR-FOM*/
    protected int physicalEntityHandle;
    
    /** Handle for the EntityID attribute */
    protected int entityIDHandle;
    
    /** Physical location, world coordinates */
    protected int locationHandle;
    
    /** A random number to assign to the federate (ourselves) when we start up */
    private short federateNumber = 0;
    

    /**
     * @param args the command line arguments  
     */
    public static void main(String[] args) 
    {
        // get a federate name, use "exampleFederate" as default and tack on a random number
        short federateNumber = (short)(Math.random() * 10000);
        String federateName = "rprFederate-" + federateNumber;
       
        try
        {
                // run the example federate
                new RprFomFederate().runFederate( federateName, federateNumber );
        }
        catch( RTIexception rtie )
        {
                // an exception occurred, just log the information and exit
                System.out.println("Top level exception caught");
                rtie.printStackTrace();
        }
    }
    
    public static void log(String message)
    {
        System.out.println(message);
    }
    
   /**
    * This is the main simulation loop. 
    */
    public void runFederate( String federateName, short federateNumber ) throws RTIexception
    {
        log("Starting up federate named " + FEDERATION_NAME);
        
        this.federateNumber = federateNumber;
        federateAmbassador = new RprFomFederateAmbassador(this);
        rtiAmbassador = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
        federateAmbassador.rtiAmbassador = rtiAmbassador;
       
        // Create a federate execution, one run of the simulation.
        try
        {
            // Read the RPR-FOM fed
            File fom = new File( "foms/RPR-FOM.fed" );
            rtiAmbassador.createFederationExecution( FEDERATION_NAME, fom.toURI().toURL() );
            log( "Created Federation " + FEDERATION_NAME + " from FOM " + fom.toURI().toURL() );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            // It's possible someone else has already created the federate execution
            System.out.println( "Joining existing federation " + FEDERATION_NAME + " as federate " + federateName );
        }
        catch( MalformedURLException urle )
        {
            // Some problem reading the fom file
            log( "Cannot read FOM: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }
        catch(Exception e)
        {
            log("Generic exception creating federation: " + e.getMessage());
        }
        
        
        // The fedration execution has been created. Now individual federates can
        // join the federation execution.
        
        rtiAmbassador.joinFederationExecution( federateName, FEDERATION_NAME, federateAmbassador );
        log( "Joined Federation " + FEDERATION_NAME + " as " + federateName );
        
        // announce a sync point, then wait for the notification of the sync point to come back to 
        // us. We can't just announce a sync point and then proceed, because the RTI must ack 
        // the creation of the sync point.
        rtiAmbassador.registerFederationSynchronizationPoint( RUN_SYNC_POINT, null );
        
        while( federateAmbassador.isAnnounced == false )
        {
                rtiAmbassador.tick();
        }
        
        // Wait for the user to hit return. This allows us to create all the federates we want first
        // and then start executing them.
        this.waitForUser();
        
        rtiAmbassador.synchronizationPointAchieved( RUN_SYNC_POINT );
        log( "Achieved sync point: " + RUN_SYNC_POINT + ", waiting for federation..." );
        while( federateAmbassador.isReadyToRun == false )
        {
            rtiAmbassador.tick();
        }
        
       // We could enable a time policy here; choose not to, run open-loop
        
        // All the data we will publish, and all the data types we will receive
        publishAndSubscribe();
        
        // Register an actual object.
        int objectHandle = registerObject();
        log( "Registered Object, handle=" + objectHandle );
        
        // Initialize a physical entity. This involves setting the entity ID
        // and any other values that should be set
       //int objectHandle = initializePhysicalEntity();
        
        
        // Update the
        for(int idx = 0; idx < 10000; idx++)
        {
            updateAttributeValues(objectHandle);
            rtiAmbassador.tick();
        }
        
        this.deleteObject(objectHandle);
        
        rtiAmbassador.resignFederationExecution( ResignAction.NO_ACTION );
        log( "Resigned from Federation" );

        // Try to destroy the federation execution. If any other federates are still
        // joined, this attempt will fail. The last federate exiting will destroy
        // the federation.
        try
        {
                rtiAmbassador.destroyFederationExecution( FEDERATION_NAME );
                log( "Destroyed Federation " + FEDERATION_NAME );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
                log( "No need to destroy federation " + FEDERATION_NAME + ", it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
                log( "Didn't destroy federation" + FEDERATION_NAME + ", federates still joined" );
        }
                
    }
    
    /** Waits for the user to hit return. This allows us to get all federates up and redevous in the
     * waiting room before the federation begins executing.
     */
    private void waitForUser()
    {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
                reader.readLine();
        }
        catch( Exception e )
        {
                log( "Error while waiting for user input: " + e.getMessage() );
                e.printStackTrace();
        }
    }
    
    public void publishAndSubscribe()
    {
        try
        {
            // Subscribe to PhysicalEntity objects and the EntityIdentifier attribute of those
            this.physicalEntityHandle = rtiAmbassador.getObjectClassHandle( "ObjectRoot.BaseEntity.PhysicalEntity" );
            this.entityIDHandle = rtiAmbassador.getAttributeHandle("EntityIdentifier", physicalEntityHandle );
            this.locationHandle = rtiAmbassador.getAttributeHandle("WorldLocation", physicalEntityHandle );
            
             // package the information into a handle set
             AttributeHandleSet attributes =
                        RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
             attributes.add( entityIDHandle );
             attributes.add( locationHandle );
            
                
             rtiAmbassador.subscribeObjectClassAttributes( physicalEntityHandle, attributes );
             System.out.println("Subscribed to object class");
             
             // Announce that we will publish PhysicalEntity objects
            rtiAmbassador.publishObjectClass(this.physicalEntityHandle, attributes);
            System.out.println("Published object class PhysicalEntity");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("Problem subscribing to attributes: " + e);
        }

    }
    
/**
 * This method will register an instance of the class ObjectRoot.BaseEntity.PhysicalEntity and will
 * return the federation-wide unique handle for that instance. Later in the
 * simulation, we will update the attribute values for this instance
 */
private int registerObject() throws RTIexception
{
    int classHandle = rtiAmbassador.getObjectClassHandle( "ObjectRoot.BaseEntity.PhysicalEntity" );
    
    return rtiAmbassador.registerObjectInstance( classHandle );
}
    
/*
 * Updates the attribute values of a physical entity.
 */
private void updateAttributeValues( int objectHandle ) throws RTIexception
{
    
        // create the necessary container and values //
        // create the collection to store the values in, as you can see
        // this is quite a lot of work
        SuppliedAttributes attributes =
           RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
        
        // get the handles
        int classHandle = rtiAmbassador.getObjectClassHandle( "ObjectRoot.BaseEntity.PhysicalEntity" );
        int eidHandle = rtiAmbassador.getAttributeHandle( "EntityIdentifier", classHandle );
        int locationHandle = rtiAmbassador.getAttributeHandle( "WorldLocation", classHandle );
        
        
        // we use EncodingHelpers to make things nice friendly for both Java and C++
        
        // The 2 X 3 = 6 byte array that will hold the (site, application, entity) triplet
        byte[] eid = new byte[6];
        
        EncodingHelpers.encodeShort(SITE_ID, eid, 0);
        EncodingHelpers.encodeShort(APPLICATION_ID, eid, 2);
        EncodingHelpers.encodeShort(entityID, eid, 4);
        
        // put the values into the collection
        attributes.add( eidHandle, eid );
        
        byte worldLocationBuffer[] = new byte[24];
        EncodingHelpers.encodeDouble(montereyLocation[0], worldLocationBuffer, 0);
        EncodingHelpers.encodeDouble(montereyLocation[1], worldLocationBuffer, 8);
        EncodingHelpers.encodeDouble(montereyLocation[2], worldLocationBuffer, 16);
        
        attributes.add(locationHandle, worldLocationBuffer);
        
        // Do the update
        rtiAmbassador.updateAttributeValues( objectHandle, attributes, generateTag() );
}

private byte[] generateTag()
{
    return (""+System.currentTimeMillis()).getBytes();
}

private void deleteObject( int handle ) throws RTIexception
{
        rtiAmbassador.deleteObjectInstance( handle, generateTag() );
}
      
}
