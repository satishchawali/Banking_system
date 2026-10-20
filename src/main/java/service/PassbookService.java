package service;
import model.BankTransaction;import java.time.LocalDate;import java.util.List;
/** Builds passbooks from persisted transaction rows, including the final balance before the range. */
public final class PassbookService {
 private final BankingService bank;public PassbookService(BankingService bank){this.bank=bank;}
 public PassbookStatement statement(String account,String pin,LocalDate from,LocalDate to){if(from.isAfter(to))throw new IllegalArgumentException("Start date must not be after end date.");var opening=bank.balanceBefore(account,pin,from);List<BankTransaction> rows=bank.history(account,pin,from,to);var closing=rows.isEmpty()?opening:rows.get(rows.size()-1).balanceAfter();return new PassbookStatement(account,from,to,opening,closing,rows);} public void exportPdf(PassbookStatement statement,java.nio.file.Path path)throws java.io.IOException{try(org.apache.pdfbox.pdmodel.PDDocument doc=new org.apache.pdfbox.pdmodel.PDDocument()){String[] lines=statement.render().replace("₹","INR ").split("\\R");for(int start=0;start<lines.length;start+=42){org.apache.pdfbox.pdmodel.PDPage page=new org.apache.pdfbox.pdmodel.PDPage();doc.addPage(page);try(org.apache.pdfbox.pdmodel.PDPageContentStream stream=new org.apache.pdfbox.pdmodel.PDPageContentStream(doc,page)){stream.beginText();stream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA,9);stream.newLineAtOffset(40,750);for(int i=start;i<Math.min(start+42,lines.length);i++){String line=lines[i].replaceAll("[^\\x20-\\x7E]","?");if(line.length()>105)line=line.substring(0,105);stream.showText(line);stream.newLineAtOffset(0,-16);}stream.endText();}}doc.save(path.toFile());}}
}



