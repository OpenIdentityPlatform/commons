package com.savage7.maven.plugin.dependency;

import com.google.api.translate.Language;
import com.google.api.translate.TranslateV2;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        // Set the HTTP referrer to your website address.
    	TranslateV2 translate = new TranslateV2();    	
    	TranslateV2.setHttpReferrer("http://localhost");

        String englishText = "Hello World";
        String spanishTranslatedText = translate.execute(englishText, Language.ENGLISH, Language.SPANISH);
        String frenchTranslatedText = translate.execute(englishText, Language.ENGLISH, Language.FRENCH);
        String germanTranslatedText = translate.execute(englishText, Language.ENGLISH, Language.GERMAN);

        System.out.println("ENLGISH : " + englishText);
        System.out.println("SPANISH : " + spanishTranslatedText);
        System.out.println("FRENCH  : " + frenchTranslatedText);
        System.out.println("GERMAN  : " + germanTranslatedText);
    }
}
