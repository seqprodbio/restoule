package models;

import javax.naming.*;
import javax.naming.directory.*;

import java.util.Hashtable;

public class LDAPAuthentication {

   public static boolean validateUser(String username, String password) {
      String userCN = "uid=" + username + ",ou=People,dc=oicr,dc=on,dc=ca";
      String groupToCheck = "cn=spbpde,ou=Groups,dc=oicr,dc=on,dc=ca";
      String ldapIpAndPort = "10.0.0.100:389";
      return validateUser(username, password, userCN, groupToCheck, ldapIpAndPort);
   }

   public static boolean validateUser(String username, String password, String userCN) {
      String groupToCheck = "cn=spbpde,ou=Groups,dc=oicr,dc=on,dc=ca";
      String ldapIpAndPort = "10.0.0.100:389";
      return validateUser(username, password, userCN, groupToCheck, ldapIpAndPort);
   }

   public static boolean validateUser(String username, String password, String userCN, String groupToCheck) {
      String ldapIpAndPort = "10.0.0.100:389";
      return validateUser(username, password, userCN, groupToCheck, ldapIpAndPort);
   }

   @SuppressWarnings("unchecked")
   public static boolean validateUser(String username, String password, String userCN, String groupToCheck, String ldapIpAndPort) {
      // Set up environment for creating initial context
      Hashtable env = new Hashtable(11);
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.PROVIDER_URL, "ldap://" + ldapIpAndPort);

      // Authenticate as none
      env.put(Context.SECURITY_AUTHENTICATION, "none");

      try {
         // Create initial context
         DirContext ctx = new InitialDirContext(env);

         // The following if statement checks if the user is in the spbpde group. It is possible that some people were not added to this
         // In that case, you may want to try adding them as a exception or trying to find a more suitable group to check
         if (checkIfInGroup(ctx, groupToCheck, username)) {
            Hashtable env2 = new Hashtable(11);
            env2.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env2.put(Context.PROVIDER_URL, "ldap://10.0.0.100:389");

            // Authenticate as user
            // Might want to change authentication later
            env2.put(Context.SECURITY_AUTHENTICATION, "simple");
            env2.put(Context.SECURITY_PRINCIPAL, userCN);
            env2.put(Context.SECURITY_CREDENTIALS, password);
            try {
               DirContext ctx2 = new InitialDirContext(env2);
               ctx2.close(); // At this point we either logged in or an error was thrown so we can just close it now
            } catch (NamingException e) {
               e.printStackTrace();
               System.out.println("Error: couldn't login user: " + username);
               ctx.close();
               return false;
            }
            System.out.println("You're in!");
            ctx.close();
            return true;
         } else {
            System.out.println("Error: user " + username + " not in group: " + groupToCheck);
            ctx.close();
            return false;
         }

      } catch (NamingException e) {
         e.printStackTrace();
         return false;
      }
   }

   public static Boolean checkIfInGroup(DirContext ctx, String groupName, String username) {
      return checkIfInGroup(ctx, groupName, username, null);
   }

   public static Boolean checkIfInGroup(DirContext ctx, String groupName, String username, String attributeIdToCheck) {
      if (attributeIdToCheck == null) {
         attributeIdToCheck = "memberUid";
      }

      try {
         Attributes attributes = ctx.getAttributes(groupName);

         for (NamingEnumeration nameEnum = attributes.getAll(); nameEnum.hasMore();) {
            Attribute currentAttribute = (Attribute) nameEnum.next();
            if (currentAttribute.getID().equals(attributeIdToCheck)) {
               for (NamingEnumeration nameEnum2 = currentAttribute.getAll(); nameEnum2.hasMore();) {
                  if (nameEnum2.next().toString().equals(username)) {
                     return true;
                  }
               }
            }
         }
      } catch (NamingException e) {
      }
      return false;
   }
}
