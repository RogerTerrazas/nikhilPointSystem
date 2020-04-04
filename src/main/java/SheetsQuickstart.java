import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.sun.source.tree.WhileLoopTree;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

import java.util.*;

public class SheetsQuickstart {
    public static Sheets sheetsService;
    public static String APPLICATION_NAME = "Point System";
    public static String SPREADSHEET_ID = "1nrTjc-12FIUwJoRe4mZ1J2DeVN5sDdCVV4BWEy9ue-I";

    public static Credential authorize()  throws IOException, GeneralSecurityException {
        InputStream in = SheetsQuickstart.class.getResourceAsStream("/credentials.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JacksonFactory.getDefaultInstance(), new InputStreamReader(in)
        );

        List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
                clientSecrets, scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver())
                .authorize("user");

        return credential;
    }

    public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        Credential credential = authorize();
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        // Connect to R.A.T.
        sheetsService = getSheetsService();

        // Pull data from sheets we need
        String paidMembersRange = "Paid Members!A2:ZZ1000";
        String pointSystemRange = "Point System!A1:ZZ1000";
        String eventAttendanceRange = "Event Attend. F19-S20!B1:ZZ1000";
        String pointDistRange = "Point Dist.!B1:ZZ1000";

        ValueRange paidMembersSheet = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, paidMembersRange).execute();
        ValueRange pointSystemSheet = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, pointSystemRange).execute();
        ValueRange eventAttendanceSheet = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, eventAttendanceRange).execute();
        ValueRange pointDistSheet = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, pointDistRange).execute();



        List<List<Object>> paidMembers = paidMembersSheet.getValues();
        List<List<Object>> pointSystem = pointSystemSheet.getValues();
        List<List<Object>> eventAttendance = eventAttendanceSheet.getValues();
        List<List<Object>> pointDist = pointDistSheet.getValues();

        pointDistSheet.setValues(pointDist);

        HashMap<String, ArrayList<Object>> paidMemberMap = new HashMap<String, ArrayList<Object>>();

        //Populate paid member mapping
        for (List<Object> paidMember : paidMembers) {
            if(paidMember.size() > 0 && !paidMember.get(3).toString().equals("")) {
                String name = paidMember.get(1).toString().toLowerCase();
                String EID = paidMember.get(3).toString().toLowerCase();
                paidMemberMap.put(EID, new ArrayList<Object>(Collections.nCopies(pointSystem.get(0).size(), "")));
                paidMemberMap.get(EID).set(0,name);
                paidMemberMap.get(EID).set(1,0);
                paidMemberMap.get(EID).set(2, "not even close");
                for(int i = 3; i < pointSystem.get(0).size(); i++){
                    paidMemberMap.get(EID).set(i, 0);
                }
            }
        }

        //overall point calculator
        for(int i = 0; i < eventAttendance.get(0).size(); i++){
            String currentEvent = eventAttendance.get(1).get(i).toString();
            System.out.println(currentEvent);
            String s1 = eventAttendance.get(0).get(i).toString();
            if(pointDist.get(1).contains(currentEvent)) {
                int index = pointDist.get(1).indexOf(currentEvent);
                int pointValue = Integer.parseInt(pointDist.get(2).get(index).toString());
                String currentEventPillar = pointDist.get(0).get(index).toString();

                try {
                    for (int k = 3; !eventAttendance.get(k).get(i).equals(""); k++) {
                        String eventEID = eventAttendance.get(k).get(i).toString().toLowerCase();
                        System.out.println(eventEID);
                        if(paidMemberMap.containsKey(eventEID)){
                            try {
                                Integer totalPoints = Integer.parseInt(paidMemberMap.get(eventEID).get(1).toString());
                                Integer pillarTotal = Integer.parseInt(paidMemberMap.get(eventEID).get(pillarIndex(currentEventPillar)).toString());
                                Integer memberEventPoints = Integer.parseInt(paidMemberMap.get(eventEID).get(index+8).toString());

                                totalPoints -= pillarTotal;
                                memberEventPoints += pointValue;
                                pillarTotal += pointValue;

                                if(pillarTotal > 30){
                                    totalPoints += 30;
                                    pillarTotal = 30;
                                }else {
                                    totalPoints += pillarTotal;
                                }
                                if(totalPoints >=100){
                                    paidMemberMap.get(eventEID).set(2, "YAS!");
                                }else if(totalPoints > 80) {
                                    paidMemberMap.get(eventEID).set(2, "ALMOST!");
                                }else if(totalPoints > 40) {
                                    paidMemberMap.get(eventEID).set(2, "EHHH");
                                }else {
                                        paidMemberMap.get(eventEID).set(2, "NOPE :(");
                                }
                                paidMemberMap.get(eventEID).set(1, totalPoints);
                                paidMemberMap.get(eventEID).set(pillarIndex(currentEventPillar), pillarTotal);
                                paidMemberMap.get(eventEID).set(index+8, memberEventPoints);
                            }catch (NumberFormatException ignored){
                            }
                        }
                    }
                } catch (IndexOutOfBoundsException ignored) {

                }
            }
        }



        //Return the Point System
        List<List<Object>> pointSystemReturn = new ArrayList<>(paidMemberMap.values());


        ValueRange requestBody = new ValueRange();
        requestBody.setValues(pointSystemReturn);

        Sheets.Spreadsheets.Values.Update request =
                sheetsService.spreadsheets().values().update(SPREADSHEET_ID, "Point System!A2:ZZ1000", requestBody);
        request.setValueInputOption("RAW");
        request.execute();


        System.out.println(paidMembers);
        System.out.println(eventAttendanceSheet);
    }
    public static int pillarIndex (String pillar) {
        if (pillar.equals("Chapter Development Total")) {
            return 3;
        } else if (pillar.equals("Leadership Development Total")) {
            return 4;
        } else if (pillar.equals("Professional Development Total")) {
            return 5;
        } else if (pillar.equals("Academic Development Total")) {
            return 6;
        } else {
            return 7;
        }
    }
}


