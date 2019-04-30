package one.chest.alexa.player

import com.amazon.speech.json.SpeechletRequestEnvelope
import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.*
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioItem
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayer
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.StopDirective
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFailedRequest
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFinishedRequest
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackNearlyFinishedRequest
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStartedRequest
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStoppedRequest
import com.amazon.speech.ui.PlainTextOutputSpeech
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import one.chest.musiclibrary.MusicLibrary
import one.chest.musiclibrary.MusicLibraryImpl
import one.chest.musiclibrary.TrackLocation

@Slf4j
@CompileStatic
class PlayerSpeechlet implements SpeechletV2, AudioPlayer {

    MusicLibraryImpl musicLibrary = (MusicLibraryImpl) MusicLibrary.createDefaultLibrary("https://music.yandex.ru")
    JsonSlurper slurper = new JsonSlurper()

    @Override
    void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {

    }

    @Override
    SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        log.info("onLaunch")
        return new SpeechletResponse(outputSpeech: new PlainTextOutputSpeech(text: 'Hello, welcome to stream application'))
    }

//    PlayerSpeechlet() {
//        Unirest.setHttpClient(
//                HttpClients
//                        .custom()
//                        .setDefaultRequestConfig(RequestConfig.custom()
//                            .setSocketTimeout(60000)
//                            .setConnectionRequestTimeout(60000)
//                            .build())
//                        .setDefaultCookieStore(new BasicCookieStore()).build()
//        )
//    }

    @Override
    SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
//        if(requestEnvelope.session.new) {
//            SpeechletResponse.newAskResponse()
//        }
        Intent intent = requestEnvelope.request.intent
        log.info("SESSION_ID: ${requestEnvelope.session.sessionId}")

        if (intent.name == 'AMAZON.PauseIntent') {
            return new SpeechletResponse(directives: [stopDirective()]);
        }

        if (intent.name == 'PlayIntent') {
            def playDirective = createPlayDirective("https://chest.one/playlist/tracks/stream", UUID.randomUUID().toString(), PlayBehavior.REPLACE_ALL)
            return new SpeechletResponse(directives: [playDirective]);
        }

        if (intent.name == 'PlayArtistSongIntent') {
            Slot song = intent.getSlot('song_name')
            Slot artist = intent.getSlot('artist_name')
            log.info("Finding $song by $artist")
            String findURL = "https://chest.one/tracks?artist=${artist.value.replace(" ", "+")}&song=${song.value.replace(" ", "+")}"
            log.info(findURL)
            String text = new URL(findURL).text
            log.info("Response = ${text}")
            def response = slurper.parseText(text)
            def trackLocation = response['trackLocation']
            String url = musicLibrary.getDownloadLink(new TrackLocation(
                    trackLocation['albumId'] as int,
                    trackLocation['trackId'] as int
            ))

            log.info("Download link is '${url}'")
            def playDirective = createPlayDirective(url, song.value + artist.value, PlayBehavior.REPLACE_ALL)
            return new SpeechletResponse(directives: [playDirective]);
        }
        log.info("onIntent")
        return new SpeechletResponse(outputSpeech: new PlainTextOutputSpeech(text: intent.name))
    }

    final private static Directive stopDirective() {
        new StopDirective()
    }

    @Override
    void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {

    }

    private static SpeechletResponse stream() {
        def url = 'https://chest.one/playlist/tracks/stream'

        def playDirective = createPlayDirective(url, 'stream', PlayBehavior.REPLACE_ALL)

        return new SpeechletResponse(directives: [playDirective])
    }

    private static Directive createPlayDirective(String href, String title, PlayBehavior behavior) {
        Stream stream = new Stream();
        if (PlayBehavior.ENQUEUE.equals(behavior)) {
            stream.setExpectedPreviousToken(title); // nicht falls PlayBehavior.REPLACE_ALL
        }
        stream.setToken(title);
        stream.setOffsetInMilliseconds(0);
        //String url = YScannerYoutubeApi.YOUTUBEHOST + href;

        long start = System.currentTimeMillis();
        String mp3Url = href
        log.info("mp3Url: " + mp3Url);
        log.info("duration: " + (System.currentTimeMillis() - start));

        stream.setUrl(mp3Url);

        AudioItem audio = new AudioItem();
        audio.setStream(stream);

        PlayDirective playDirective = new PlayDirective();
        playDirective.setAudioItem(audio);
        playDirective.setPlayBehavior(behavior);
        log.info("Directive: $playDirective")
        return playDirective;
    }

    @Override
    SpeechletResponse onPlaybackFailed(SpeechletRequestEnvelope<PlaybackFailedRequest> requestEnvelope) {
        log.info("onPlaybackFailed(intent: ${requestEnvelope.request.error.message}")
        return new SpeechletResponse(outputSpeech: new PlainTextOutputSpeech(text: "Playback failed: ${requestEnvelope.request.error.message}"))
    }

    @Override
    SpeechletResponse onPlaybackFinished(SpeechletRequestEnvelope<PlaybackFinishedRequest> requestEnvelope) {
        log.info("onPlaybackFinished(intent: ${requestEnvelope.request.offsetInMilliseconds}")
        return new SpeechletResponse(outputSpeech: new PlainTextOutputSpeech(text: "Playback finished in ${requestEnvelope.request.offsetInMilliseconds}ms"))
    }

    @Override
    SpeechletResponse onPlaybackNearlyFinished(SpeechletRequestEnvelope<PlaybackNearlyFinishedRequest> requestEnvelope) {
        log.info("onPlaybackFinished(intent: ${requestEnvelope.request.offsetInMilliseconds}")
        return new SpeechletResponse(outputSpeech: new PlainTextOutputSpeech(text: "Playback nearly finished in ${requestEnvelope.request.offsetInMilliseconds}ms"))
    }

    @Override
    SpeechletResponse onPlaybackStarted(SpeechletRequestEnvelope<PlaybackStartedRequest> requestEnvelope) {
        log.info("onPlaybackFinished(intent: ${requestEnvelope.request.offsetInMilliseconds}")
        return new SpeechletResponse(outputSpeech: new PlainTextOutputSpeech(text: "Playback started: ${requestEnvelope.request.offsetInMilliseconds}ms"))
    }

    @Override
    SpeechletResponse onPlaybackStopped(SpeechletRequestEnvelope<PlaybackStoppedRequest> requestEnvelope) {
        return null
    }
}
