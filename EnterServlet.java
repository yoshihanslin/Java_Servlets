package org.server.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;

/**
 * Servlet implementation class EnterServlet
 */
@WebServlet("/EnterServlet")
public class EnterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	//public static String msg = "";  //message needed to show on the site
	public static final String ORG_MSG = "Hello New User!";  //original message
	public static final String COOKIE_NAME = SessCookieManage.COOKIE_NAME;  //cookie name
	public static final int SESSION_TIMEOUT_SECS = SessCookieManage.SESSION_TIMEOUT_SECS;
	public static final int SESSION_TIMEOUT_DELTA_SECS = SessCookieManage.SESSION_TIMEOUT_DELTA_SECS;
	//public static final SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
	public static int sess_num = 0;  //the valid session number start from 1
	public static String svrIDLocal;  //local server ID, initialized at "void init(ServletConfig config)"
	public static String svrIDBackup;
	//public static String svrIDBackupTest;  //mock server ID, initialized as 127.0.0.2

	//information which needed to deliver to the client
	//public static String cookieInfo = "";  //all information of a cookie
	//public static String sessExpiration = "";  //session expiration-timestamp

	// Client 
	public static RPCClient rpcClient = new RPCClient();
	
	public static boolean isAWS = true;  //whether the program runs in AWS
	public static boolean exchangedViewsWithSimpleDB = false;
	// session table
	// key: session ID
	// value: version number + "_" + msg + "_" + descardTime;
	public static Map<String, String> SessTbl = new ConcurrentHashMap<String, String>();
	public static Map<String, String> ServerView = new ConcurrentHashMap<String, String>();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public EnterServlet() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	static SessTblGarbageThread tSess = new SessTblGarbageThread();
	static RPCServerThread tRPCServer = new RPCServerThread();
	static ViewServerThread tViewServer = new ViewServerThread();
	static ViewClientThread tViewClient = new ViewClientThread();
	static Semaphore waitForViewThreadsInitialized = new Semaphore(1);
	static boolean initialized = false;
	
	/*
	static SessTblGarbageThread tSess = new SessTblGarbageThread();
	static RPCServerThread tRPCServer = new RPCServerThread();
	static ViewServerThread tViewServer = new ViewServerThread();
	static ViewClientThread tViewClient = new ViewClientThread();
	static Semaphore waitForViewThreadsInitialized = new Semaphore(0);
	
	static boolean initialized = false;
	*/
	//public static void putInSimpleDB(){
		/*
		if(!initialized){
			//get local server IDs
			int count=10;
			while (count >0) {
				try {
					svrIDLocal = getSvrIDLocal(); //by Runtime.exec() in EC2 instances
					break;
				} catch (IOException io) {
					System.out.println("Error getting runtime svrIDLocal");
					count--;
				}
			}
			ViewClientThread.putItemInSimpleDB(svrIDLocal, "up");
			
			if (!isAWS) {
				ViewClientThread.deleteItemInSimpleDB("127.0.0.1");
				ViewClientThread.putItemInSimpleDB("127.0.0.1", "up");
				ViewClientThread.sdb.deleteAttributes(new DeleteAttributesRequest(ViewClientThread.domainName, "127.0.0.2"));
				ViewClientThread.sdb.deleteAttributes(new DeleteAttributesRequest(ViewClientThread.domainName, "127.0.0.3"));
				ViewClientThread.sdb.deleteAttributes(new DeleteAttributesRequest(ViewClientThread.domainName, "127.0.0.5"));
			}
			
			waitForViewThreadsInitialized.release();
			initialized = true;
		}
		*/
	//}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	
	public void init() throws ServletException {
	//public void init(ServletConfig config) throws ServletException {
		  // Initialization code...
		
			
			//waitForViewThreadsInitialized.release();
		//@TODO: store svrIDLocal to simpleDB
		
		// *****************************test: svrIDPrimary & svrIDBackup ************************//	
		//svrIDLocal = "127.0.0.1";
		//svrIDBackupTest = RPCClient.SVRID_NULL;
		//svrIDBackupTest = "127.0.0.3";
		//putInView(svrIDLocal, "up");
		
	}
	
	
		/*  	set date format and provide parsing functions 
		between strings and date values in a thread-safe manner
	*/
	//ThreadLocal variable to hold the DateFormat object
	private static final ThreadLocal<DateFormat> sdf
	             = new ThreadLocal<DateFormat>(){
	@Override
	protected DateFormat initialValue() {
	    return new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
	}
	};
	
	public Date stringToDate(String s)
	                 throws ParseException{
	Date dt = sdf.get().parse(s);
	return dt;
	}
	public String dateToString(Date d)
	      throws ParseException{
	  String s = sdf.get().format(d); 
	  return s;
	}



	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(!initialized){
			
			//get local server IDs
			int count=10;
			while (count >0) {
				try {
					svrIDLocal = getSvrIDLocal(); //by Runtime.exec() in EC2 instances
					break;
				} catch (IOException io) {
					System.out.println("Error getting runtime svrIDLocal");
					count--;
				}
			}
			
			putInView(svrIDLocal, "up");
			
			//simpleDB created in ViewClientThread.init2()
			ViewClientThread.init2();
			ViewClientThread.putItemInSimpleDB(svrIDLocal, "up"); 
		
		SessTblGarbageThread tSess = new SessTblGarbageThread();
		RPCServerThread tRPCServer = new RPCServerThread();
		ViewServerThread tViewServer = new ViewServerThread();
		ViewClientThread tViewClient = new ViewClientThread();
		
		/*
		Semaphore waitForViewThreadsInitialized = new Semaphore(1,true);
		try {
			waitForViewThreadsInitialized.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		// initialize ViewClient thread
		tViewClient.start();
		
		tSess.start();

		// initialize RPC server thread
		tRPCServer.start();
		
		// initialize ViewServer thread
		tViewServer.start();
	
		try {
		Thread.sleep(3000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		if (exchangedViewsWithSimpleDB== false) {
		tViewClient.ViewExchangeWithSimpleDB("1"+svrIDLocal);
		exchangedViewsWithSimpleDB=true;
		}
		
		svrIDBackup= ViewServerThread.findRandomServer();
		
		while (svrIDBackup==null || svrIDBackup.equals(RPCClient.SVRID_NULL) || svrIDBackup.equals("null")) {
			try {
				svrIDLocal = getSvrIDLocal(); //by Runtime.exec() in EC2 instances
				break;
			} catch (IOException io) {
				System.out.println("Error getting runtime svrIDLocal");
			}
			putInView(svrIDLocal, "up");
			ViewClientThread.putItemInSimpleDB(svrIDLocal, "up");
			if (exchangedViewsWithSimpleDB== false) {
				tViewClient.ViewExchangeWithSimpleDB("1"+svrIDLocal);
				exchangedViewsWithSimpleDB=true;
				}
			svrIDBackup= ViewServerThread.findRandomServer();
		}

				if (!isAWS) {
					ViewClientThread.deleteItemInSimpleDB("127.0.0.1");
					ViewClientThread.putItemInSimpleDB("127.0.0.1", "up");
					ViewClientThread.sdb.deleteAttributes(new DeleteAttributesRequest(ViewClientThread.domainName, "127.0.0.2"));
					ViewClientThread.sdb.deleteAttributes(new DeleteAttributesRequest(ViewClientThread.domainName, "127.0.0.3"));
					ViewClientThread.sdb.deleteAttributes(new DeleteAttributesRequest(ViewClientThread.domainName, "127.0.0.5"));
				}
	
		
		
		
		
						
				//waitForViewThreadsInitialized.release();
				
				initialized = true;
	}

		Cookie[] cookies = request.getCookies();
		Cookie sessCookie = null;  //cookie needed to pass to client
		boolean isNewClient = true;  // true if this is a new client, 
									//cookie name is not "CS5300PROJ1SESSION"
		SiteInfo siteInfo = new SiteInfo();  //initialize the information that need to pass to webpage
		siteInfo.setSiteSvrIDRequest(svrIDLocal);

		//whether one of the cookie has the name "CS5300PROJ1SESSION"
		if (cookies != null) {
			for (Cookie cookie: cookies) {
				if (COOKIE_NAME.equals(cookie.getName())) {
					isNewClient = false;
					sessCookie = cookie;
				}
			}
		}

		// !isNewClient: this is NOT a NEW client
		if (!isNewClient) {
			
			//retrieveData = foundVersion + "_" + foundData
			String retrieveData = retrieveSession(sessCookie, siteInfo);
			
			//successfully retrieve sessData in SessTbl
			if(null != retrieveData){
				String[] sessDetails =retrieveData.split("_");
				String msg = sessDetails[1].trim();
				storeSession(false, sessCookie, msg, siteInfo);  //update an old session
				response.addCookie(sessCookie);  // pass the cookie to browser
				
				//infomation pass the the webpage
				siteInfo.setSiteMsg(msg);
				siteInfo.setSiteSvrIDPrimary(SessCookieManage.getServerIDPrimary(sessCookie));
				siteInfo.setSiteSvrIDBackup(SessCookieManage.getServerIDBackup(sessCookie));
			}//if(retrieveSession(sessCookie))
			
			else {  //CANNOT retrieve session in SessTbl
				response.addCookie(sessCookie);  // pass the cookie to browser
				String msg = "doGet: CANNOT retrieve sessData in SessTbl";
				
				//infomation pass the the webpage
				siteInfo.setSiteNotRetrieve(msg);
			}
			
		}//if (!isNewClient)
		
		else {  // this is a NEW client
			
			// create a new cookie
			sessCookie = SessCookieManage.createCookie(); 
			
			// update SessTbl
			String msg = ORG_MSG;
			storeSession(true, sessCookie, msg, siteInfo);  //create a new session
			response.addCookie(sessCookie);
			
			//infomation pass the the webpage
			siteInfo.setSiteMsg(msg);
			siteInfo.setSiteSvrIDPrimary(SessCookieManage.getServerIDPrimary(sessCookie));
			siteInfo.setSiteSvrIDBackup(SessCookieManage.getServerIDBackup(sessCookie));
		}//else

		//Information pass to the webpage
		String siteView = printView();
		System.out.println(siteView);
		request.setAttribute("siteMsg", siteInfo.getSiteMsg());
		request.setAttribute("siteSvrIDRequest", siteInfo.getSiteSvrIDRequest());
		request.setAttribute("siteSvrIDFound", siteInfo.getSiteSvrIDFound());
		request.setAttribute("siteSvrIDFoundPB", siteInfo.getSiteSvrIDFoundPB());
		request.setAttribute("siteSvrIDPrimary", siteInfo.getSiteSvrIDPrimary());
		request.setAttribute("siteSvrIDBackup", siteInfo.getSiteSvrIDBackup());
		request.setAttribute("siteExpirationTime", siteInfo.getSiteExpirationTime());
		request.setAttribute("siteDiscardTime", siteInfo.getSiteDiscardTime());
		request.setAttribute("siteView", siteView);
		
		String svrLocalIDEC2= getSvrIDLocal();
		
		  response.setContentType("text/html");
		    PrintWriter out = response.getWriter();
		 
		    String docType =
		      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 " +
		      "Transitional//EN\">\n";
	/*
		    out.println
		      (docType +
		    		  "<HTML>\n" +
				       "<HEAD><TITLE>" + svrIDLocal+ "</TITLE></HEAD>\n" +
				       "<BODY BGCOLOR=\"#FDF5E6\">\n" +
				       "<H1>" + svrLocalIDEC2 + "</H1>\n"+
				       "<H2>" + getSvrIDLocal() + "</H2>\n"+
				       "<H2>" + "svrIDBackup "+ svrIDBackup+ "</H2>\n"+
				       "</BODY></HTML>");
		
		*/
		    /*
		 out.println
	      (docType + 
	    		  "<HTML>\n" +
			       "<HEAD><TITLE>" + "UI"+ "</TITLE></HEAD>\n" +"<BR>"+"\n"+
			       "<BODY><H1>"+ (String)request.getAttribute("siteMsg") +"</H1><HR/>"+"<BR>"+"\n"+
			       " <FORM NAME=\"response\" ACTION=\"EnterServlet\" METHOD=\"POST\" >"+"<BR>"+"\n"+
			       " <INPUT TYPE=\"hidden\" NAME=\"pagename\" VALUE=\"reponse\" >"+"<BR>"+"\n"+
			       " <TABLE CELLPADDING=\"5\"  CELLSPACING= \"5\">" +"<BR>"+"\n"+
			       "<TR> <TD><BUTTON TYPE=\"SUBMIT\"  NAME=\"BUTTON\" VALUE=\"BUTTONREPLACE\">" + "Replace"+ "</BUTTON></TD> "+"<BR>"+"\n"+
			       "<TD><INPUT TYPE=\"TEXT\"  NAME=\"TXTREPLACE\" /></TD>"+"\n"+
			       "</TR>"+"\n"+
			       "<TR>"+"\n"+
			       "<TD><BUTTON TYPE=\"SUBMIT\"  NAME=\"BUTTON\" VALUE=\"BUTTONREFRESH\">" + "Refresh"+ "</BUTTON></TD> "+"<BR>"+"\n"+
			       "</TR>"+"\n"+
			       "<TR>"+"<TD><BUTTON TYPE=\"SUBMIT\"  NAME=\"BUTTON\" VALUE=\"BUTTONLOGOUT\">" + "Logout"+ "</BUTTON></TD> "+"<BR>"+"\n"+
			       "</TR>"+"<BR>"+"\n"+
			       "</TABLE>"+"<BR>"+"\n"+
			       "</FORM>"+"<BR>"+"\n"+
			       "<P>"+"<BR>"+"\n"+
			       "Server executing the client request: "+ (String)request.getAttribute("siteSvrIDRequest")+"<BR>"+"\n"+
			       "Session data is found in: " + (String)request.getAttribute("siteSvrIDFound")+"<BR><BR>"+"\n"+
			       "Session Information" + "<br>"+"\n"+
			       "SvrID_primary: " + (String)request.getAttribute("siteSvrIDPrimary") + "<br>"+"\n"+
			       "SvrID_backup: " + (String)request.getAttribute("siteSvrIDBackup") + "<br>"+"\n"+
			       "Expiration Time: " + (String)request.getAttribute("siteExpirationTime") + "<br>"+"\n"+
			       "Discard Time: " + (String)request.getAttribute("siteDiscardTime") + "<br><br>"+"\n"+
			       "View Information" + "<br>"+ (String)request.getAttribute("siteView")+"</p>"+"\n"+
			       "</BODY></HTML>");
		*/
		RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
		dispatcher.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		int count=10;
		while (count >0) {
			try {
				svrIDLocal = getSvrIDLocal(); //by Runtime.exec() in EC2 instances
				break;
			} catch (IOException io) {
				System.out.println("Error getting runtime svrIDLocal");
				count--;
			}
		}
		
		putInView(svrIDLocal, "up");
		if (ViewClientThread.sdb==null) {
			initialized = false;
			doGet(request,response);
			//ViewClientThread.init2();
			//ViewClientThread.putItemInSimpleDB(svrIDLocal, "up"); 
		}
		//simpleDB created in ViewClientThread.init2()
		//ViewClientThread.init2();
		putInView(ViewClientThread.domainName, "up");
		//ViewClientThread.putItemInSimpleDB(svrIDLocal, "up"); 
		
		Cookie[] cookies = request.getCookies();
		Cookie sessCookie = null;  //cookie needed to pass to client
		boolean isNewClient = true;  // true if this is a new client, 
									//cookie name is not "CS5300PROJ1SESSION"
		SiteInfo siteInfo = new SiteInfo();  //initialize the information that need to pass to webpage
		siteInfo.setSiteSvrIDRequest(svrIDLocal);

		//whether one of the cookie has the name "CS5300PROJ1SESSION"
		if (cookies != null) {
			for (Cookie cookie: cookies) {
				if (COOKIE_NAME.equals(cookie.getName())) {
					isNewClient = false;
					sessCookie = cookie;
				}
			}
		}

		// !isNewClient: this is NOT a NEW client
		if (!isNewClient) {

			//retrieveData = foundVersion + "_" + foundData
			String retrieveData = retrieveSession(sessCookie, siteInfo);
			
			//can retrieve sessData in SessTbl
			if(null != retrieveData){  
				String btnParam = request.getParameter("button");
				//button: refresh
				if (btnParam.equals("buttonRefresh")) {
					String[] sessDetails = retrieveData.split("_");
					String msg = sessDetails[1].trim();
					storeSession(false, sessCookie, msg, siteInfo);  //update an old session
					response.addCookie(sessCookie);  // pass the cookie to browser
					
					//infomation pass the the webpage
					siteInfo.setSiteMsg(msg);
					siteInfo.setSiteSvrIDPrimary(SessCookieManage.getServerIDPrimary(sessCookie));
					siteInfo.setSiteSvrIDBackup(SessCookieManage.getServerIDBackup(sessCookie));
				}
				//button: buttonReplace
				else if(btnParam.equals("buttonReplace")) {
					String msg = request.getParameter("txtReplace");
					storeSession(false, sessCookie, msg, siteInfo);  //update an old session
					response.addCookie(sessCookie);  // pass the cookie to browser
					
					//infomation pass the the webpage
					siteInfo.setSiteMsg(msg);
					siteInfo.setSiteSvrIDPrimary(SessCookieManage.getServerIDPrimary(sessCookie));
					siteInfo.setSiteSvrIDBackup(SessCookieManage.getServerIDBackup(sessCookie));
				}
				//button: buttonLogout
				else if(btnParam.equals("buttonLogout")) {
					SessTbl.remove(SessCookieManage.getSessionID(sessCookie));
					sessCookie.setMaxAge(0);
					response.addCookie(sessCookie);
					
					//infomation pass the the webpage
					siteInfo.setSiteTimeout();
				}
			}//if(retrieveSession(sessCookie))
			
			else { //CANNOT retrieve sessData in SessTbl
				response.addCookie(sessCookie);  // pass the cookie to browser
				String msg = "doPost: CANNOT retrieve sessData in SessTbl";
				
				//infomation pass the the webpage
				siteInfo.setSiteNotRetrieve(msg);
			}
			
		}//if (!isNewClient)
		
		else {  // this is a NEW client
			
			// create a new cookie
			sessCookie = SessCookieManage.createCookie(); 
			
			// update SessTbl
			String msg = ORG_MSG;
			storeSession(true, sessCookie, msg, siteInfo);
			response.addCookie(sessCookie);
			
			//infomation pass the the webpage
			siteInfo.setSiteMsg(msg);
			siteInfo.setSiteSvrIDPrimary(SessCookieManage.getServerIDPrimary(sessCookie));
			siteInfo.setSiteSvrIDBackup(SessCookieManage.getServerIDBackup(sessCookie));
		}//else
		
		if((siteInfo.getSiteSvrIDPrimary()).equals("[CANNOT RETRIEVE]")) siteInfo.setSiteSvrIDPrimary(svrIDLocal);
		String svrBupID="[CANNOT RETRIEVE]";
		if((siteInfo.getSiteSvrIDBackup()).equals("[CANNOT RETRIEVE]")) {
			ViewClientThread vc = new ViewClientThread();
			svrBupID = vc.findRandomServerOrSimpleDB();
			svrBupID = ViewServerThread.findRandomServer();
			if (svrBupID==null || svrBupID.equals(RPCClient.SVRID_NULL) || svrBupID.equals("null")) {
					vc.ViewExchangeWithSimpleDB("1"+svrIDLocal);
					svrBupID = ViewServerThread.findRandomServer();
			}
		}
		siteInfo.setSiteSvrIDBackup(svrBupID);
		if (ServerView.containsKey("SVRIDNULL")) ServerView.remove("SVRIDNULL");
		
		//Information pass to the webpage
		String siteView = printView();
		System.out.println(siteView);
		request.setAttribute("siteMsg", siteInfo.getSiteMsg());
		request.setAttribute("siteSvrIDRequest", siteInfo.getSiteSvrIDRequest());
		request.setAttribute("siteSvrIDFound", siteInfo.getSiteSvrIDFound());
		request.setAttribute("siteSvrIDFoundPB", siteInfo.getSiteSvrIDFoundPB());
		request.setAttribute("siteSvrIDPrimary", siteInfo.getSiteSvrIDPrimary());
		request.setAttribute("siteSvrIDBackup", siteInfo.getSiteSvrIDBackup());
		request.setAttribute("siteExpirationTime", siteInfo.getSiteExpirationTime());
		request.setAttribute("siteDiscardTime", siteInfo.getSiteDiscardTime());
		request.setAttribute("siteView", siteView);
		
		String svrLocalIDEC2= getSvrIDLocal();
		
		  response.setContentType("text/html");
		    PrintWriter out = response.getWriter();
		
		    String docType =
		      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 " +
		      "Transitional//EN\">\n";
	/*
		    out.println
		      (docType +
		    		  "<HTML>\n" +
				       "<HEAD><TITLE>" + svrIDLocal+ "</TITLE></HEAD>\n" +
				       "<BODY BGCOLOR=\"#FDF5E6\">\n" +
				       "<H1>" + svrLocalIDEC2 + "</H1>\n"+
				       "<H2>" + getSvrIDLocal() + "</H2>\n"+
				       "<H2>" + "svrIDBackup "+ svrIDBackup+ "</H2>\n"+
				       "</BODY></HTML>");
		
		*/
		    /*
		 out.println
	      (docType + 
	    		  "<HTML>\n" +
			       "<HEAD><TITLE>" + "UI"+ "</TITLE></HEAD>\n" +"<BR>"+"\n"+
			       "<BODY><H1>"+ (String)request.getAttribute("siteMsg") +"</H1><HR/>"+"<BR>"+"\n"+
			       " <FORM NAME=\"response\" ACTION=\"EnterServlet\" METHOD=\"POST\" >"+"<BR>"+"\n"+
			       " <INPUT TYPE=\"hidden\" NAME=\"pagename\" VALUE=\"reponse\" >"+"<BR>"+"\n"+
			       " <TABLE CELLPADDING=\"5\"  CELLSPACING= \"5\">" +"<BR>"+"\n"+
			       "<TR> <TD><BUTTON TYPE=\"SUBMIT\"  NAME=\"BUTTON\" VALUE=\"BUTTONREPLACE\">" + "Replace"+ "</BUTTON></TD> "+"<BR>"+"\n"+
			       "<TD><INPUT TYPE=\"TEXT\"  NAME=\"TXTREPLACE\" /></TD>"+"\n"+
			       "</TR>"+"\n"+
			       "<TR>"+"\n"+
			       "<TD><BUTTON TYPE=\"SUBMIT\"  NAME=\"BUTTON\" VALUE=\"BUTTONREFRESH\">" + "Refresh"+ "</BUTTON></TD> "+"<BR>"+"\n"+
			       "</TR>"+"\n"+
			       "<TR>"+"<TD><BUTTON TYPE=\"SUBMIT\"  NAME=\"BUTTON\" VALUE=\"BUTTONLOGOUT\">" + "Logout"+ "</BUTTON></TD> "+"<BR>"+"\n"+
			       "</TR>"+"<BR>"+"\n"+
			       "</TABLE>"+"<BR>"+"\n"+
			       "</FORM>"+"<BR>"+"\n"+
			       "<P>"+"<BR>"+"\n"+
			       "Server executing the client request: "+ (String)request.getAttribute("siteSvrIDRequest")+"<BR>"+"\n"+
			       "Session data is found in: " + (String)request.getAttribute("siteSvrIDFound")+"<BR><BR>"+"\n"+
			       "Session Information" + "<br>"+"\n"+
			       "SvrID_primary: " + (String)request.getAttribute("siteSvrIDPrimary") + "<br>"+"\n"+
			       "SvrID_backup: " + (String)request.getAttribute("siteSvrIDBackup") + "<br>"+"\n"+
			       "Expiration Time: " + (String)request.getAttribute("siteExpirationTime") + "<br>"+"\n"+
			       "Discard Time: " + (String)request.getAttribute("siteDiscardTime") + "<br><br>"+"\n"+
			       "View Information" + "<br>"+ (String)request.getAttribute("siteView")+"</p>"+"\n"+
			       "</BODY></HTML>");
			       */
		RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
		dispatcher.forward(request, response);
	}
	
	/**
	 * Retrieve session value in SessTbl in local or remote server
	 * @param _cookie: cookie from browser
	 * @return: (if session exist) foundVersion + "_" + foundData
	 * 			(if not) null
	 */
	public String retrieveSession(Cookie _cookie, SiteInfo _siteInfo) {
		
		// *****************************test: svrIDPrimary & svrIDBackup ************************//
		String tmpSvrIDLocal;
		if (isAWS) {  //it runs in AWS
			tmpSvrIDLocal = svrIDLocal;
		}else { //it doesn't run in AWS
			tmpSvrIDLocal = "testtest";
		}
		
		//either SvrIDPrimary or SvrIDBackup is the receiving server's own SvrID
		if (SessCookieManage.getServerIDPrimary(_cookie).equals(tmpSvrIDLocal) 
				|| SessCookieManage.getServerIDBackup(_cookie).equals(tmpSvrIDLocal)) {

			String retrieveData;
			try {
				// Retrieve Session State from SvrIDLocal
				// sessData = version + "_" + msg + "_" + descardTime
				String sessData = SessTbl.get(SessCookieManage.getSessionID(_cookie));
				if (null != sessData) { //Retrieve Session State locally
					String[] sessDetails = sessData.split("_");
					retrieveData = sessDetails[0] + "_" + sessDetails[1];
					
					//infomation pass to the webpage
					_siteInfo.setSiteSvrIDFound(tmpSvrIDLocal);
					if (SessCookieManage.getServerIDPrimary(_cookie).equals(tmpSvrIDLocal)) {
						_siteInfo.setSiteSvrIDFoundPB("primary server");
					}else {
						_siteInfo.setSiteSvrIDFoundPB("backup server");
					}
					return retrieveData;
				}
				
				else { // Retrieve Session State from sendSvrIDAnother
					String sendSvrIDAnother;
					if (SessCookieManage.getServerIDPrimary(_cookie).equals(svrIDLocal)) {
						sendSvrIDAnother = SessCookieManage.getServerIDBackup(_cookie);
					}else {
						sendSvrIDAnother = SessCookieManage.getServerIDPrimary(_cookie);
					}
					rpcClient.clearDestAddrs();
					rpcClient.addDestAddr(sendSvrIDAnother);
					
					DatagramPacket recvPkt = rpcClient.SessionRead(SessCookieManage.getSessionID(_cookie));
					if (null != recvPkt) {  //successful retrieve session
						retrieveData = RPCClient.getSessValueFromRecvPkt(recvPkt);
						
						//infomation pass to the webpage
						_siteInfo.setSiteSvrIDFound(sendSvrIDAnother);
						if (SessCookieManage.getServerIDPrimary(_cookie).equals(tmpSvrIDLocal)) {
							_siteInfo.setSiteSvrIDFoundPB("backup server");
						}else {
							_siteInfo.setSiteSvrIDFoundPB("primary server");
						}
						return retrieveData;
					}else {
						// set status as down in view
						ViewServerThread.setServerDown(sendSvrIDAnother); 
						return null;
					}
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} 
			return null;
			
		}else {  // NEITHER SvrIDPrimary NOR SvrIDBackup is the receiving server's own SvrID
			String retrieveData;
			
			// Retrieve Session State on other server
			try {
				// Retrieve Session State from SvrIDPrimary
				rpcClient.clearDestAddrs();
				rpcClient.addDestAddr(SessCookieManage.getServerIDPrimary(_cookie));
				DatagramPacket recvPkt1 = rpcClient.SessionRead(SessCookieManage.getSessionID(_cookie));
				if (null != recvPkt1) {  //successful retrieve session
					retrieveData = RPCClient.getSessValueFromRecvPkt(recvPkt1);
					
					//infomation pass to the webpage
					_siteInfo.setSiteSvrIDFound(SessCookieManage.getServerIDPrimary(_cookie));
					_siteInfo.setSiteSvrIDFoundPB("primary server");
					return retrieveData;
				}else {
					// set status as down in view
					ViewServerThread.setServerDown(SessCookieManage.getServerIDPrimary(_cookie));
					
					// Retrieve Session State from SvrIDBackup
					rpcClient.clearDestAddrs();
					rpcClient.addDestAddr(SessCookieManage.getServerIDBackup(_cookie));
					DatagramPacket recvPkt2 = rpcClient.SessionRead(SessCookieManage.getSessionID(_cookie));
					if (null != recvPkt2) {  //successful retrieve session
						retrieveData = RPCClient.getSessValueFromRecvPkt(recvPkt2);
						
						//infomation pass to the webpage
						_siteInfo.setSiteSvrIDFound(SessCookieManage.getServerIDBackup(_cookie));
						_siteInfo.setSiteSvrIDFoundPB("backup server");
						return retrieveData;
					}else {
						// set status as down in view
						ViewServerThread.setServerDown(SessCookieManage.getServerIDBackup(_cookie));
						return null;
					}
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	/**
	 * store session state to svrIDPrimary and svrIDBackup
	 * @param _isNew: whether this is a new client
	 * @param _cookie: cookie from browser
	 * @param _msg: message from browser
	 */
	public void storeSession(boolean _isNew, Cookie _cookie, String _msg, SiteInfo _siteInfo) {
		
		try {
			if (!_isNew){  // update version for valid session
				SessCookieManage.setVersion(_cookie, SessCookieManage.getVersion(_cookie) + 1);
			}
			SessCookieManage.setServerIDPrimary(_cookie, svrIDLocal);
			
			//****************** unfinished: look for svrIDBackup in view ********************//
			//here, we also need to consider no new_backup can be found
			//server chooses an "up" random ServerID (excluding simpleDB) from View, with probability near (1/View_size)
			svrIDBackup = ViewServerThread.findRandomServer();
			while(null != svrIDBackup) {

			    SessCookieManage.setServerIDBackup(_cookie, svrIDBackup);
				_cookie.setMaxAge(SESSION_TIMEOUT_SECS);

				//set SessTbl message and expiration-timestamp 
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, _cookie.getMaxAge());
				String sessExpiration = cal.getTime().toString();

				// Storing Session State on primary(local) server, put data to local SessTbl
				cal.add(Calendar.SECOND, SESSION_TIMEOUT_DELTA_SECS);
				String descardTime = cal.getTime().toString();
				String sessData = SessCookieManage.getVersion(_cookie) + "_" + _msg 
						+ "_" + descardTime;
				SessTbl.put(SessCookieManage.getSessionID(_cookie), sessData);
				
				// Storing Session State on remote backup server
				rpcClient.clearDestAddrs();
				rpcClient.addDestAddr(SessCookieManage.getServerIDPrimary(_cookie));
				rpcClient.addDestAddr(SessCookieManage.getServerIDBackup(_cookie));
				
				//rpc call
				DatagramPacket writePkt = rpcClient.SessionWrite(SessCookieManage.getSessionID(_cookie), 
						SessCookieManage.getVersion(_cookie), _msg, descardTime);
				
				if (null != writePkt) { //success SessionWrite then break
					break;
				} 
				else { // null == writePkt
					ViewServerThread.setServerDown(svrIDBackup);  // set status as down in view
					svrIDBackup = ViewServerThread.findRandomServer();
				}
			} //while(null != svrIDBackup)
			
			//null == svrIDBackup
			if (null == svrIDBackup) {
				svrIDBackup = RPCClient.SVRID_NULL;
			}

			//**************** test: set svrIDBackup as svrIDBackupTest ****************//
		    //SessCookieManage.setServerIDBackup(_cookie, svrIDBackupTest);
		    SessCookieManage.setServerIDBackup(_cookie, svrIDBackup);
			
			_cookie.setMaxAge(SESSION_TIMEOUT_SECS);

			//set SessTbl message and expiration-timestamp 
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, _cookie.getMaxAge());
			String sessExpiration = cal.getTime().toString();

			// Storing Session State on primary(local) server, put data to local SessTbl
			cal.add(Calendar.SECOND, SESSION_TIMEOUT_DELTA_SECS);
			String descardTime = cal.getTime().toString();
			String sessData = SessCookieManage.getVersion(_cookie) + "_" + _msg 
					+ "_" + descardTime;
			SessTbl.put(SessCookieManage.getSessionID(_cookie), sessData);
			
			//infomation pass to the webpage
			_siteInfo.setSiteSvrIDPrimary(svrIDLocal);
			_siteInfo.setSiteSvrIDBackup(svrIDBackup);
			_siteInfo.setSiteExpirationTime(sessExpiration);
			_siteInfo.setSiteDiscardTime(descardTime);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
	}
	
	//@hans
	public static String getSvrIDLocal() throws IOException{
		
		if (isAWS) {  //it runs in AWS
			String svrIDLocal;
		    Runtime rt = Runtime.getRuntime();
		    Process proc;

		    proc= rt.exec("/opt/aws/bin/ec2-metadata --public-ipv4");
		    	System.out.println("capture IP command error");
		    BufferedReader input = new BufferedReader(
		    		new InputStreamReader(proc.getInputStream()));

		    BufferedReader error = new BufferedReader(new 
		         InputStreamReader(proc.getErrorStream()));

		    // read the output from the command
		    String s = null;
		    StringBuilder resp = new StringBuilder();

		    while ((s = input.readLine()) != null) {
		        resp.append(s);
		    }

		    //IP addr returned as:  public-ipv4: ww.xx.yy.zz
		    String[] sArr = (resp.toString()).split(" ");
		    svrIDLocal = sArr[1].trim();

		    // read any errors from the attempted command
		    s=null;
		    while ((s = error.readLine()) != null) {
		        System.out.println(s);
		    }
		    System.out.println("svrLocalIP "+svrIDLocal);
		    
		    input.close();
		    error.close();
		    return svrIDLocal;
		}
		
		else {  //it runs locally
			String svrIDLocal=null;
			URL url;
			BufferedReader br;
			try {
				url = new URL("http://checkip.amazonaws.com/");
				br = new BufferedReader(new InputStreamReader(url.openStream()));
				svrIDLocal = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			svrIDLocal = InetAddress.getLocalHost().getHostAddress().toString();
			return svrIDLocal;
		}
	}
	
	/**
	 * @return: view message
	 */
	public static String printView() {
		String out = null;
		Iterator<Map.Entry<String,String>> itr = EnterServlet.ServerView.entrySet().iterator();
		System.out.println("ServerView: ServerID = status-timestamp");

		while (itr.hasNext()) {
			Map.Entry<String,String> pair = itr.next();
			//outView = ServerID-Status-Timestamp_ServerID-Status-Timestamp_ ...
			if (out==null) out=pair.getKey() + " = " + pair.getValue();
			else out = out + "<BR>" + pair.getKey() + " = " + pair.getValue();

			//System.out.println(pair.getKey() + " = " + pair.getValue());
		}
		return out;
	}
	
	//@hans
	//ServerView Tuple= ServerID, status-timestamp: key=ServerID, value=status-timestamp
	//putInView(svrIDLocal, "up");
	//return server status= up or down
	public static String viewGetStatus(String value){
		String[] s =value.split("-");
		return s[0]; 
	}
	
	//@hans
	//return server status timestamp
	public static String viewGetTimestamp(String value){
		String[] s =value.split("-");
		return s[1]; 
	}
	
	public void putInView(String sID, String stat) {
		try {
			if (svrIDLocal!=null && (!svrIDLocal.equals("null")) &&
					(!svrIDLocal.equals(RPCClient.SVRID_NULL))
					&& (!svrIDLocal.equals("SVRIDNULL"))) {
			ServerView.put(sID, stat+"-"+(dateToString(new Date(System.currentTimeMillis()))));
					}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		if (svrIDLocal==null || svrIDLocal.equals("null")||svrIDLocal.equals(RPCClient.SVRID_NULL)
				|| svrIDLocal.equals("SVRIDNULL")) ServerView.remove(svrIDLocal);
		
	}
}
