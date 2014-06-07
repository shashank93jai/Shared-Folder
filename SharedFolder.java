import java.io.*;
import java.net.*;
import java.util.*;

class Tools{
	public static ArrayList<String> splitStringPipe(ArrayList<String> x){
		ArrayList<String> ret = new ArrayList<String> ();
		for(int i=0;i<x.size();i++){
			int j =x.get(i).indexOf('|');
			String str = new String();
			str = x.get(i).substring(0,j);
			//System.out.println(str);
			ret.add(str);
		}
		return ret;
	}
	public static String splitString(String x){
		int j = x.indexOf('|');
		String str = new String();
		str = x.substring(0,j);
		return str;
	}
}
class Network{
	public static String sharedDir = new String();
	public static int serverPort = 1031;
	public static int clientPort = 1031;
	public static int bufferSize = 3000000;
	public static DatagramSocket ds;
	public static String ip = new String();
	
	public static byte buffer[] = new byte[bufferSize];
	public DatagramPacket data;
	public static boolean isLeader;
	public String broadcastAddress;
	
	public void close(){
		ds.close();
	}
	
	public String getMyIp(){
		return ip;
	}
	public Network(String s){
		try {
			/*
			Network initialisation
			*/
			sharedDir = s;
			isLeader = true;
			try {
				ds = new DatagramSocket(serverPort);
				data = new DatagramPacket(buffer, bufferSize);
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				while (interfaces.hasMoreElements()){
					NetworkInterface current = interfaces.nextElement();
					if(!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
					Enumeration<InetAddress> addresses = current.getInetAddresses();
					while(addresses.hasMoreElements()){
						InetAddress current_addr = addresses.nextElement();
						if(current_addr instanceof Inet4Address)
							ip = current_addr.getHostAddress();
						else if(current_addr instanceof Inet6Address)
							ip = current_addr.getHostAddress();
					}
				}
				int idx = ip.lastIndexOf('.');
				broadcastAddress = ip.substring(0, idx)+".255";
				byte buf[] = new byte[90];
				buf[0] = (byte)0;
				send(buf);
	//			System.out.println("hell "+ broadcastAddress);
			}
			catch(SocketException se){
	//			System.out.println("caught :::"+se);
			}
			
			
		}
		catch(Exception se){
			System.out.println("caught Network"+se);
		}
	}
	public Network(int x){
		data = new DatagramPacket(buffer, buffer.length);
	}
	public void control() {
		try {
			if((data.getData())[0] == (byte)0){
				if( isLeader ){
					System.out.println("Giving up leadership");
					ArrayList <String> arr = new ArrayList <String>();
					FileManager f = new FileManager(this); 
					arr = f.getFilesWithModificationTime(sharedDir, 0);
					f.sync(arr, 0);
					isLeader = false;
				}
			}
		}
		catch(Exception e){
		}
	}
	public DatagramPacket receive(){
		try {
		//	data = new DatagramPacket(buffer, buffer.length);
			while(true){
				ds.receive(data);
				if(this.getMyIp().equals(data.getAddress().toString().substring(1)))continue;
				if(data.getLength() > 90){
					break;
				}
				else {
					this.control();
				}
			}
		}
		catch(Exception e){
		}
		return data;
	}
	public void send(byte[] b){
		//System.out.println("Send function : "+b.length);
		try {
			Thread.sleep(500);
		
			ds.send(new DatagramPacket(b,b.length,InetAddress.getByName(broadcastAddress),clientPort));
		}
		catch(Exception e){
			System.out.println("caught Network send"+e);
		}
	}
}

class FPP{
	/*
	Folder Protocol Packet
	*/
	
	/*
	format : 
	
	[20,50) ---> file name
	[100,) ---> file contents
	[19,19] ---> one bit value ( set if the file has to be created and unset if has to be deleted )
	
	*/
	
	private byte[] header = new byte [100];
	private byte[] contents = new byte[0];
	private String fileName=new String();
	
	private int size;
	private int fl;
	
	public FPP(byte[] incomingData, int incomingSize){
	
		try{
			//System.out.println(" "+incomingData.length +"      hd    " + incomingSize );
			try{
			for(int i=0;i<100;i++){
				header[i] = incomingData[i];
			}
			}
			catch(Exception e){
				System.out.println("first +  "+e);
			}
		
			contents = new byte[incomingSize-100];
			
			try{
			for(int i=100;i<incomingSize;i++){
				contents[i-100]=incomingData[i];
			}
			}
			catch(Exception e){
				System.out.println("second +  "+e);
			}
			
			fileName = "";
			try{
			for(int i=20;true;i++){
				if(header[i]=='\0')break;
				fileName = fileName + Character.toString((char) header[i]);
			}
			}
			catch(Exception e){
				System.out.println("third +  "+e);
			}
		}
		catch (Exception e){
			System.out.println("FPP COns : "+e);
		}
	}
	
	public FPP(){
		size=0;fl=0;
		header = new byte[100];
	}
	
	public void setFileCreate(){
		header[19]=1;
	}
	public void setFileDelete(){
		header[19]=0;
	}
	public boolean toBeDeleted(){
		if(header[19]==0)return true;
		return false;
	}
	
	public byte[] toByteArray(){
		byte [] ret = new byte[1];
		
		try {
		ret = new byte[header.length + contents.length];
		
		for(int i=0;i<100;i++)ret[i]=header[i];
		
		for(int i=0;i<contents.length;i++)ret[100+i]=contents[i];
		}
		catch (Exception e){
			System.out.println(" to byte array : "+ e);
		}
		return ret;
		
	}
	
	public void setFileName(String s){
		for(int i=0;i<s.length();i++){
			header[20+i]=(byte)s.charAt(i);
		}
		header[20+s.length()]='\0';
		fileName = s;
	}
	
	public String getFileName(){
		return fileName;
	}
	
	public void setFileContents(byte[] buf){
		contents = new byte[buf.length];
		for(int i=0;i<buf.length;i++){
			contents[i]=buf[i];
		}
		size=100+buf.length;
	}
	
	public byte[] getFileContents(){
		return contents;
	}
}




class Monitor extends Thread{
	String sharedDir;
	FileManager filemanager;
	Network nw = new Network(1);
	public static ArrayList<String> pre = new ArrayList<String> ();
	public static ArrayList<String> toBeDel = new ArrayList<String> ();
	public static ArrayList<String> toBeAdded = new ArrayList<String> ();
	public Monitor(String arg, Network nww){
		sharedDir = new String();
		sharedDir = arg;
		nw = nww;
	}
	
	
	public static void addToExistingFiles(String fileName){
		for(int i=0;i<pre.size();i++){
			if(pre.get(i).equals(fileName)){
				return;
			}
		}
		pre.add(fileName);
	}
	
	public static void deleteFromExistingFiles(String fileName){
		ArrayList <String> t1 = new ArrayList<String> ();
		t1= Tools.splitStringPipe(pre);
		
		ArrayList <String > temp = new ArrayList <String> ();
		for(int i=0;i<t1.size();i++){
			
			if(t1.get(i).equals(fileName)){
				continue;
			}
			temp.add(pre.get(i));
		}
		
		pre=new ArrayList<String> (temp);
	}
	
	public void run(){
		ArrayList <String> files = new ArrayList<String>();
		filemanager = new FileManager(nw);
		try{
			while(true){
				toBeAdded = new ArrayList<String> ();
				toBeDel = new ArrayList<String> ();
				Thread.sleep(5000);
				files = filemanager.getFilesWithModificationTime(sharedDir, 0);
				
				toBeAdded = new ArrayList<String>();
				for(int i=0;i<files.size();i++){
					int fl=0;
					for(int j=0;j<pre.size();j++){
						if( (pre.get(j) ).equals( files.get(i) ) ){
							fl=1;break;
						}
					}
					if(fl==0){
						toBeAdded.add(files.get(i));
					}
				}
				
				for(int i=0;i<pre.size();i++){
					int fl=0;
					for(int j=0;j<files.size();j++){
						if( ( Tools.splitString(pre.get(i)) ).equals( Tools.splitString(files.get(j)) ) ){
							fl=1;break;
						}
					}
					if(fl==0){
						toBeDel.add(pre.get(i));
					}
				}
				
				
				filemanager.sync(toBeDel,1);
				filemanager.sync(toBeAdded,0);
				
				pre=files;
			}
		}
		catch (Exception e){
			System.out.println("GOtcha Monitor:"+e);
		}
	}
}

class Reciever extends Thread{
	public static Network nw=new Network(1);
	public static FileManager filemanager;
	public static String sharedDir=new String();
	public Reciever(Network nww, String x){
		nw=nww;
		sharedDir = x;
	}
	
	public void run(){
		try{
			
			String my_ip =nw.getMyIp();
			filemanager = new FileManager(nw);
			
			while(true){
			
				DatagramPacket p = nw.receive();
			//	System.out.println("I recieved ");
				String src_ip = p.getAddress().toString();
				src_ip = src_ip.substring(1,src_ip.length());
				
				if(src_ip.equals(my_ip))continue;
				
				FPP fp = new FPP (p.getData(), p.getLength());
				
				// do the appropriate action here;
				//
				/*
				The code goes here
				
				*/
				
				//If the file has to be deleted delte it.
				
				if( fp.toBeDeleted() ){
					filemanager.deleteFile(fp.getFileName());
					continue;
				}
				
	//			System.out.println("Receiving file : "+fp.getFileName());
				filemanager.writeFile(fp.getFileName(), fp.getFileContents());
			
			
			}
		}
	
		catch (Exception e){
			System.out.println("GOtcha Reciever : "+e);
		}
	}
	
}


class FileManager{
	
	public static int latestTime;
	public static Network nw = new Network(1);
	public static int lock;
	
	public FileManager(Network nww){
		FileManager.latestTime = 0;
		nw = nww;
		lock =0;
	}
	public static long getModificationTime(String url) throws Exception {
		Runtime runtime = Runtime.getRuntime();
		Process p;
		p = runtime.exec("date -r "+ url + " +%s");
		p.waitFor();
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line ="";
		long time = 0;
		line = in.readLine();
		time = Long.parseLong(line);
		return time;
	}
	public void setLatestTime(int lT){
		latestTime = lT;
	}

	
	public ArrayList<String> getFilesWithModificationTime(String url, long lastSyncTime) throws Exception{
		Runtime runtime = Runtime.getRuntime();
		Process p ;
 		p = runtime.exec("find "+url);
		p.waitFor();
 			
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line="";

                ArrayList<String> files = new ArrayList<String>();
                ArrayList<String> toBeSyncedFiles = new ArrayList<String>();
                while((line=in.readLine()) != null) 
                {
         		//p = runtime.exec("date -r"+line+" +%s");
         		files.add(line);
                }
                int i=0;
                long maxModificationTime=0;
		ArrayList<String> result = new ArrayList<String>();
                for(; i < files.size(); i++)
                {
			String temp = files.get(i);
			String command = "date -r "+ temp + " +%s";
//			System.out.println(command);			
			in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			p = runtime.exec("date -r "+ temp + " +%s");
			while((line=in.readLine()) != null) 
	                {	
	                	long time = Long.parseLong(line);
				result.add(new String(temp+"|"+line));
	                	if(time > lastSyncTime)
	                	{
	                		toBeSyncedFiles.add(temp);
					if(time > maxModificationTime)
					{
						maxModificationTime = time;
					}
	                	}
        	        }
        	        in.close();
                }
		return result;
	}
	
	
	
	
	public ArrayList<String> getFiles(String url, long lastSyncTime) throws Exception{
		Runtime runtime = Runtime.getRuntime();
		Process p ;
 		p = runtime.exec("find "+url);
		p.waitFor();
 			
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line="";

                ArrayList<String> files = new ArrayList<String>();
                ArrayList<String> toBeSyncedFiles = new ArrayList<String>();
                while((line=in.readLine()) != null) 
                {
         		//p = runtime.exec("date -r"+line+" +%s");
         		files.add(line);
                }
                int i=0;
                long maxModificationTime=0;
                
                for(; i < files.size(); i++)
                {
			String temp = files.get(i);
			String command = "date -r "+ temp + " +%s";
//			System.out.println(command);			
			in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			p = runtime.exec("date -r "+ temp + " +%s");
			while((line=in.readLine()) != null) 
	                {	
	                	long time = Integer.parseInt(line);
	                	if(time > lastSyncTime)
	                	{
	                		toBeSyncedFiles.add(temp);
					if(time > maxModificationTime)
					{
						maxModificationTime = time;
					}
	                	}
        	        }
        	        in.close();
                }
		return toBeSyncedFiles;
	}
	
	
	
	public void sync(ArrayList<String> files, int delete){
		files=Tools.splitStringPipe(files);
		if(delete==1){
			FPP filePacket=new FPP();
			for(int i=0;i<files.size();i++){
				filePacket.setFileName(files.get(i));
				filePacket.setFileDelete();
				nw.send(filePacket.toByteArray());
			}
			return;
			
		}
		ArrayList<String> temp = new ArrayList<String> ();
		for(int i=0;i<files.size();i++){
			int fl=0;
			for(int j=0;j<files.get(i).length();j++){
				if(files.get(i).charAt(j)=='.'){
					fl=1;
				}
			}
			if(fl==1)temp.add(files.get(i));
		}
		files = new ArrayList<String> (temp);
		
		try{
			FPP filePacket = new FPP();
			
			for(int i=0;i<files.size();i++){
	//			System.out.println("sync loop begin :"+files.get(i));
				File file = new File (files.get(i));
				byte[] byteFile = new byte[(int)file.length()];
				FileInputStream ff = new FileInputStream(file);
				ff.read(byteFile);
				ff.close();
				try{
					filePacket.setFileName(files.get(i));
					filePacket.setFileContents(byteFile);
					filePacket.setFileCreate();
				}
				catch ( Exception e){
					System.out.println("FPP ERROR :  " +e);
				}
				try{
				//	System.out.println("nw send: "+ files.get(i));
					nw.send(filePacket.toByteArray());
				}
				catch (Exception e){
					System.out.println(" mine send : " + e);
					
				}
				
			}
		}
		catch (Exception e){
			System.out.println("Gotcha Sync : "+e);
		}
	}
	
	public void writeFile(String url, byte[] buffer){
		try
		{
			
			
			Runtime runtime = Runtime.getRuntime();
			try{
				Process p = runtime.exec("bash script.sh "+url);	
				p.waitFor();
			}	
			catch(Exception e)
			{
		//		System.out.println("here");
			}
			try{
				FileOutputStream file = new FileOutputStream(url);
				file.write(buffer);
				file.close();
				String temp = new String();
				temp = url + "|" + String.valueOf(FileManager.getModificationTime(url));	
				Monitor.addToExistingFiles(temp);
			}
			catch(Exception e)
			{
		//		System.out.println("asdadsa");
			}
		}
		
		catch(Exception e)
		{
			System.out.println("Gotcha writeFIle: " + e);
		}
	}
	
	public void deleteFile(String url){
		try{
		//	System.out.println("Deleting File or Folder in your system :" + url);
			Monitor.deleteFromExistingFiles(url);
			boolean isDir=true;
			for(int i=0;i<url.length();i++){
				if(url.charAt(i)=='.')isDir=false;
			}
			Runtime runtime = Runtime.getRuntime();
			Process p ;
	 		
			if(isDir){
				try{
				p = runtime.exec("rm -R "+url);
				p.waitFor();
				}
				catch (Exception e){
					
				}
			}
			else{
				try{
				p = runtime.exec("rm "+url);
				p.waitFor();
				}
				catch(Exception e){
					
				}
			}
		}
		catch (Exception e){
			System.out.println("File Manager ::deleteFile(String url : " + e );
		}
		
	}
}

class SharedFolder extends Thread{
	static String sharedDir;
	
	public static void main(String args[]) throws Exception{
		sharedDir = new String (args[0]);
		Network nw = new Network(sharedDir);
		
		Monitor mon = new Monitor(SharedFolder.sharedDir,nw);
		mon.start();
		
		Reciever rec = new Reciever(nw,SharedFolder.sharedDir);
		rec.start();
		
		BufferedReader in = new BufferedReader(new InputStreamReader	(System.in));
			
		while(true){
			String str = new String();
			str = in.readLine();
			if(str.equals("quit")){
				break;
			}
			
		}
		nw.close();
	}
}


