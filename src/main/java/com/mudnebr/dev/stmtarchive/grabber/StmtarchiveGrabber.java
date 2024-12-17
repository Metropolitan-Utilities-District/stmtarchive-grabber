/*
 */
package com.mudnebr.dev.stmtarchive.grabber;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Ax Martinelli / e92060
 */
public class StmtarchiveGrabber {

	public static final String URL = "https://stmtarchivews.mudnebr.com/stmtarchivews/GetBillPDF";

	private static String outputFile;

	private static String contractNumber;
	private static String billDate;
	private static String documentNumber;

	public static void main(String[] args) {
		System.out.println("==========================================");
		System.out.println("     STATEMENT ARCHIVE GRABBER (v0.2)     ");
		System.out.println("==========================================");
		System.out.println();
		System.out.println();
		Scanner s = new Scanner(System.in);
		System.out.println("Please enter the contract account number.");
		System.out.print("     ");
		contractNumber = s.nextLine();
		System.out.println();
		System.out.println("Please enter the print document number.");
		System.out.print("     ");
		documentNumber = s.nextLine();
		System.out.println();
		System.out.println("Please enter the billing date.");
		System.out.print("     ");
		billDate = s.nextLine();
		System.out.println();
		System.out.println("What should the pdf be named?");
		System.out.print("     ");
		outputFile = s.nextLine();
		run();
	}


	public static void run() {
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
					System.err.println("Either the information you entered was incorrect, or you may need to provide more information.");
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
					System.out.println("The PDF was found, and was saved successfully.");
					Files.deleteIfExists(f.toPath());
				} else {
					System.err.println("An unexpected error occurred! The response from the server was:");
					System.err.println(line);
					System.err.println("Please send Ax Martinelli (on the AppDev team) an email about this.");
					System.err.println("Thank you! :D");
				}
			}

		} catch (IOException ex) {
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
