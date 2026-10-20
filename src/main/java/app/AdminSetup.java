package app;
import database.DatabaseConnection;import org.mindrot.jbcrypt.BCrypt;import java.io.Console;import java.sql.*;
/** One-time local utility. Run only from a trusted console to create an administrator. */
public final class AdminSetup {
 public static void main(String[] args)throws Exception{if(args.length!=1)throw new IllegalArgumentException("Usage: AdminSetup <username>");Console console=System.console();if(console==null)throw new IllegalStateException("Run this utility from a terminal so the password is not echoed.");char[] first=console.readPassword("New administrator password (minimum 12 characters): ");if(first==null||first.length<12)throw new IllegalArgumentException("Password must be at least 12 characters.");String hash=BCrypt.hashpw(new String(first),BCrypt.gensalt(12));try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO administrators(username,password_hash,active,created_at) VALUES(?,?,TRUE,CURRENT_TIMESTAMP)")){p.setString(1,args[0]);p.setString(2,hash);p.executeUpdate();}console.printf("Administrator %s created.%n",args[0]);java.util.Arrays.fill(first,'\0');}
}
