/*
 */
package com.mudnebr.dev.stmtarchive.grabber;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 *
 * @author Ax Martinelli / e92060
 */
@CommandLine.Command(name = "stmtgrab", footer = "Made by Ax Martinelli / e92060, created 2024", description = "Fetches PDFs from the STMT_ARCHIVE databases. Requires Java 21 or higher.", mixinStandardHelpOptions = true, version = "0.1")
public class StmtarchiveGrabber implements Runnable {

	public static final String URL = "https://stmtarchivews.mudnebr.com/stmtarchivews/GetBillPDF";

	@Option(names = {"-o", "--output"}, description = "The file name of the saved PDF")
	private String outputFile;

	@Option(names = {"-c", "--contract"}, description = "The contract account number")
	private String contractNumber;

	@Option(names = {"-t", "--date"}, description = "The billing date")
	private String billDate;

	@Option(names = {"-d", "--document"}, description = "The print document number")
	private String documentNumber;

	public static void main(String[] args) {
		int exitCode = new CommandLine(new StmtarchiveGrabber()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public void run() {
		if(!isValidInputs(contractNumber, documentNumber, billDate)) {
			System.err.println("Please provide at least one parameter to search.");
			System.exit(1);
		}
		Runtime rt = Runtime.getRuntime();
		StringBuilder curlString = new StringBuilder();
		curlString.append("""
                    <soapenv:Envelope
                      xmlns:ns0="http://services.saws.mudnebr.com/"
                      xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                      xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                    	<soapenv:Header>
                    	</soapenv:Header>
                    	<soapenv:Body>
                    		<ns0:GetBillStmtPDF xmlns:ns0="http://services.saws.mudnebr.com/">
                    """);

		curlString.append("			");
		if (contractNumber == null || contractNumber.isBlank()) {
			curlString.append("<ContractAccountNum/>\n");
		} else {
			curlString.append("<ContractAccountNum>").append(contractNumber).append("</ContractAccountNum>\n");
		}

		curlString.append("			");
		if (billDate == null || billDate.isBlank()) {
			curlString.append("<BillDate/>\n");
		} else {
			curlString.append("<BillDate>").append(billDate).append("</BillDate>\n");
		}

		curlString.append("			");
		if (documentNumber == null || documentNumber.isBlank()) {
			curlString.append("<InvoiceNumber/>\n");
		} else {
			curlString.append("<InvoiceNumber>").append(documentNumber).append("</InvoiceNumber>\n");
		}

		curlString.append("""
							</ns0:GetBillStmtPDF>
						</soapenv:Body>
					</soapenv:Envelope>
                    """);

		String curlDat = curlString.toString();

		File f = new File("wrk");
		
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
			pw.print(curlDat);
			pw.flush();
			pw.close();

			Process p = rt.exec(new String[]{"cmd.exe", "/c", "curl", "-X", "POST", "-H", "\"Content-Type: text/xml\"", "-d", "@wrk", "https://stmtarchivews.mudnebr.com/stmtarchivews/GetBillPDF"});
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = input.readLine()) != null) {
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(line));
				Document doc = db.parse(is);
				//check for an error
				NodeList nodes = doc.getElementsByTagName("faultstring");
				if(nodes.item(0) != null) {
					System.err.println("No file matching these parameters could be found!");
					Files.deleteIfExists(f.toPath());
					System.exit(0);
				}
				//otherwise,
				nodes = doc.getElementsByTagName("return");
				if(nodes.item(0) != null) {
					byte[] fraw = Base64.getDecoder().decode(nodes.item(0).getTextContent());
					FileOutputStream out = new FileOutputStream(outputFile == null ? "result.pdf" : outputFile + ".pdf");
					out.write(fraw);
					out.close();
					System.out.println("Saved file.");
					Files.deleteIfExists(f.toPath());
				} else {
					System.err.println("An unexpected error occurred! The response from the server was:");
					System.err.println(line);
				}
			}
			int exitVal = p.waitFor();
			System.out.println("Exited with code " + exitVal);

		} catch (IOException ex) {
			Logger.getLogger(StmtarchiveGrabber.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(StmtarchiveGrabber.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ParserConfigurationException ex) {
			Logger.getLogger(StmtarchiveGrabber.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SAXException ex) {
			Logger.getLogger(StmtarchiveGrabber.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private static boolean isValidInputs(String cnum, String inum, String bd) {
		if((bd == null || bd.isBlank()) && (cnum == null || cnum.isBlank()) && (inum == null || inum.isBlank())) {
			return false;
		}
		else return true;
	}
}
