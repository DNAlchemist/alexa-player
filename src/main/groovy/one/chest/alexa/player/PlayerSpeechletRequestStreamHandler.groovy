package one.chest.alexa.player

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler
import groovy.transform.CompileStatic

@CompileStatic
class PlayerSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {

    private static final Set<String> supportedApplicationIds = new HashSet<String>();

    static {
        supportedApplicationIds.add("amzn1.ask.skill.e1b721fe-11d5-4087-b6a5-58870d6dd0af");
    }

    PlayerSpeechletRequestStreamHandler() {
        super(new PlayerSpeechlet(), supportedApplicationIds);
    }
}