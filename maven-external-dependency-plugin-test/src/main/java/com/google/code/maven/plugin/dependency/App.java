package com.google.code.maven.plugin.dependency;

import com.google.api.translate.Language;
import com.google.api.translate.Translate;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        
        
        // Set the HTTP referrer to your website address.
        Translate.setHttpReferrer("http://localhost");

        String englishText = "Hello World";
        String spanishTranslatedText = Translate.execute(englishText, Language.ENGLISH, Language.SPANISH);
        String frenchTranslatedText = Translate.execute(englishText, Language.ENGLISH, Language.FRENCH);
        String germanTranslatedText = Translate.execute(englishText, Language.ENGLISH, Language.GERMAN);

        System.out.println("ENLGISH : " + englishText);
        System.out.println("SPANISH : " + spanishTranslatedText);
        System.out.println("FRENCH  : " + frenchTranslatedText);
        System.out.println("GERMAN  : " + germanTranslatedText);
    }
}
