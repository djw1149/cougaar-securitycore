package org.cougaar.core.security.policy.enforcers.util;

import java.util.*;

import dummy.ontology.*;
import dummy.util.CypherSuite;
import dummy.util.CypherSuiteWithAuth;

import kaos.ontology.repository.TargetInstanceDescription;

/**
 * By definition, everything in here is a temporary hack that should
 * go.  The code is not complete until this class is obsolete. Some
 * hacks are worse than others.  Every member of this class should
 * come with a comment explaining what needs to be done to get rid of
 * it.
 */
public class HardWired {
    /**
     * The string of users is so that the Servlet Enforcer can say
     * that he has some actors that he is enforcing policy for.  This
     * should go when some combination of the following are fixed:
     * <ul>
     * <li> there is a user login that is hooked to the enforcer,
     * <li> there is an alternative policy distribution scheme and
     * <li> we are using SemanticMatchers to determine if a user is in
     * a role.
     */
    public final static String [] users
	= {"http://localhost/~redmond/Extras/Names.daml#Tom",
	   "http://localhost/~redmond/Extras/Names.daml#Dick",
	   "http://localhost/~redmond/Extras/Names.daml#Harry"};

    public final static String [] agents
	= {"EnclaveOneWorkerA", "EnclaveOneWorkerB", "EnclaveOneWorkerC",
	   "EnclaveOneWorkerD", "EnclaveOneWorkerE"};

    /**
     * There will need to ultimately be a mechanism that grabs roles
     * from the system.  It could be that this will ultimately be in
     * an ontology but this has only happened when adequate support
     * exists so that the system names roles using the ontology.
     */

    public final static String kaosRoleFromRole(String role)
    {
	return 
	    dummy.ontology.ActorClassesConcepts.ActorClassesDamlURL
	    + role;
    }

    /**
     * This one actually might not be too bad.  But there will need to be some
     * scheme for translating verbs as detected in messages to verbs defined by 
     * the ontologies.
     */
    public final static String  kaosVerbFromVerb(String verb)
    {
	return (EntityInstancesConcepts.EntityInstancesDamlURL
		+ verb);
    }


    /**
     * I don't think that servlets belong in an ontology.  Possibly a
     * naming convention that converts names of servlets into names
     * that can be used in daml would work.  A consequence is that
     * some sort of dynamic registering could happen with the Domain
     * Manager so that the appropriate information is available when
     * the Domain Manager needs it.
     */
    public final static Map uriMap;
    static {
	uriMap = new HashMap();
	uriMap.put("http://www.policy.super.com/$EnclaveEight/Policy",
		   EntityInstancesConcepts.EntityInstancesDamlURL
		      +  "PolicyServlet");
	uriMap.put("http://www.CAisUS.com/$EnclaveThree/CA",
		   EntityInstancesConcepts.EntityInstancesDamlURL
		      +  "CAServlet");
    }

    /**
     * We need to map high level names for encryption schemes to what
     * they mean at a lower level.  This definitely should not be done
     * this low a level of abstraction.  It could be done in a policy
     * ontology somewhere with a naming scheme convention that people
     * can use to relate the ontology names for crypto schemes to the
     * real names.
     */
    public static final Set nsaApproved;
    static {
	nsaApproved = new HashSet();
	nsaApproved.add(new CypherSuite("3DES","RSA","MD5"));
    }

    public static final Set nsaApprovedWithAuth;
    static {
	nsaApprovedWithAuth = new HashSet();
	nsaApproved.add( new CypherSuiteWithAuth("3DES",
						 "RSA",
						 "MD5",
						 CypherSuiteWithAuth.authCertificate));
    }

    public static final Set secretCrypto;
    static {
	secretCrypto = new HashSet();
	secretCrypto.add(new CypherSuite("DES","RSA","MD5"));
    }


    public static final Set weakCrypto;
    static {
	weakCrypto = new HashSet();
	
	weakCrypto.add(new CypherSuite("DES",
				       "None",
				       "None"));
	weakCrypto.add(new CypherSuite("Plaintext",
				       "None",
				       "None"));
    }

    public static final Set weakCryptoWithAuth;
    static {
	weakCryptoWithAuth = new HashSet();
	
	weakCryptoWithAuth.add(
		 new CypherSuiteWithAuth("DES",
					 "None",
					 "None",
					 CypherSuiteWithAuth.authPassword));
	weakCryptoWithAuth.add(
		 new CypherSuiteWithAuth("Plaintext",
					 "None",
					 "None",
					 CypherSuiteWithAuth.authPassword));
	weakCryptoWithAuth.add(
		 new CypherSuiteWithAuth("Plaintext",
					 "None",
					 "None",
					 CypherSuiteWithAuth.authNoAuth));
    }


    public final static Set ulCiphersFromKAoSProtectionLevel(Set ciphers)
    {
	Set ulCiphers = new HashSet();
	for(Iterator cipherIt = ciphers.iterator(); cipherIt.hasNext();) {
	    String cipher = (String) cipherIt.next();
	    if (cipher.equals(EntityInstancesConcepts.EntityInstancesDamlURL
			      + "WeakProtection") ) {
		ulCiphers.addAll(weakCrypto);
	    } else if (cipher.equals(EntityInstancesConcepts.EntityInstancesDamlURL
				     + "NSAApprovedProtection")) {
		ulCiphers.addAll(nsaApproved);
	    } else if (cipher.equals(EntityInstancesConcepts.EntityInstancesDamlURL
				     + "SecretProtection")) {
		ulCiphers.addAll(secretCrypto);
	    } else {
		continue;  // I guess he is getting less than he wanted...
	    }
	}
	return ulCiphers;
    }

    /**
     * This will later be calculated by the directory service.
     */
    public final static HashSet hasSubjectValues;
    static {
	hasSubjectValues = new HashSet();
	hasSubjectValues.add(
	    EntityInstancesConcepts.EntityInstancesDamlURL
	    + "GetWater");
	hasSubjectValues.add(
	    EntityInstancesConcepts.EntityInstancesDamlURL
	    + "GetLogSupport");
    }

    /**
     * This will later be calculated by the directory service.
     */
    public final static HashSet usedProtectionLevelValues;
    static {
	usedProtectionLevelValues = new HashSet();
	usedProtectionLevelValues.add(
	    EntityInstancesConcepts.EntityInstancesDamlURL
	    + "WeakProtection");
	usedProtectionLevelValues.add(
	    EntityInstancesConcepts.EntityInstancesDamlURL
	    + "NSAApprovedProtection");
	usedProtectionLevelValues.add(
	    EntityInstancesConcepts.EntityInstancesDamlURL
	    + "SecretProtection");
    }


    /**
     * This will later be calculated by the directory service.
     */
    public final static HashSet usedAuthenticationLevelValues;
    static {
	usedAuthenticationLevelValues = new HashSet();
	usedAuthenticationLevelValues.add(
	    EntityInstancesConcepts.EntityInstancesDamlURL
	    + "Weak");
	usedAuthenticationLevelValues.add(
	    EntityInstancesConcepts.EntityInstancesDamlURL
	    + "NSAApproved");
    }

    /**
     * Somewhere we will need to keep a mapping from the ontology
     * authentication levels and the crypto suites
     */
    public final static HashMap usedAuthenticationLevelMap;
    static {
	usedAuthenticationLevelMap = new HashMap();
	usedAuthenticationLevelMap.put(
          EntityInstancesConcepts.EntityInstancesDamlURL
	  + "Weak",
	  weakCrypto);
	usedAuthenticationLevelMap.put(
	  EntityInstancesConcepts.EntityInstancesDamlURL
	  + "NSAApproved",
	  nsaApproved);
    }

    /**
     * This matching of the real cryptographic concepts with the
     * ontology concepts will take some work.
     */
    public static boolean addCypherSuiteWithAuthTarget(Set targets, 
						    CypherSuiteWithAuth c)
    {
	boolean weak   = weakCryptoWithAuth.contains(c);
	boolean strong = nsaApprovedWithAuth.contains(c);

	if (weak && strong) { return true; } 
	else if (weak) {
	    Set weakAuth = new HashSet();
	    weakAuth.add((Object) (EntityInstancesConcepts.EntityInstancesDamlURL
				   + "Weak"));
	    targets.add(
		new TargetInstanceDescription(
		        UltralogActionConcepts._usedAuthenticationLevel_,
			weakAuth));
	    return true;
	} else if (strong) {
	    Set strongAuth = new HashSet();
	    strongAuth.add(
		  (Object) (EntityInstancesConcepts.EntityInstancesDamlURL
			    + "NSAApproved"));
	    targets.add(
		new TargetInstanceDescription(
		        UltralogActionConcepts._usedAuthenticationLevel_,
			strongAuth));
	    return true;
	} else {
	    return false;
	}

    }
}
