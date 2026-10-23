# Offline Chinese voice input

2026-10-23 · Added to the current unpublished 0.1.12 Beta source.

The physical device was observed to have no standard Android RecognitionService. This integration therefore bundles Vosk Android 0.3.75, JNA 5.18.1, and the official small Chinese 0.22 model. The official Android demo currently uses these Vosk/JNA versions; both upstream repositories were non-archived when checked. See dependencies.json for public origins, hashes, ABI targets, maintenance snapshot and license selections. These are actual bundled runtime/model dependencies, rather than research-only references.

The model ZIP is unpacked into app-private storage on first use. Android AudioRecord supplies mono PCM16 at 16 kHz to the local recognizer. The production capture loop stores only short in-memory frames; it does not create an audio recording or call a network service. Partial hypotheses only update the visible transcript. A bounded final result fills the existing chat draft, or enters the same native sendMessage path when the user enables direct execution. Each utterance is bound to the starting chat session. Cancel, navigation, manual send, and Activity background invalidate the old result. The recorder is stopped on cancellation/background and released in the worker finally block. Runtime microphone permission is requested through the Android dialog, never granted by QA automation.

The javac/D8 build remains in use: prepare.py verifies the pinned downloads, extracts two classes.jar files and only arm64-v8a/x86_64 JNI libraries, and rejects native LOAD alignment below 16 KB. The official model ZIP is included as an asset. The APK grows substantially because this path works without a cloud ASR service or a separately installed recognizer.

`tests/run-voice-unit.sh` covers final-only submission, cancellation, old callbacks, repeated final results, foreground-only delivery, bounded text and multi-goal text. Syntax and javac checks do not prove real microphone quality. The official small Chinese model reports significant character-error rates; actual names, accents, numbers, long commands and the physical device microphone still need acceptance testing.

Debug-only native probe: launch QaActivity with mode=voice_asr. It loads the actual bundled native model without microphone permission. If the fixed app-private file `files/qa-voice-input.pcm` exists, it decodes that bounded 16 kHz mono PCM16LE public test sample and writes `files/voice-asr-contracts.json`. Without the file it verifies only model construction and leaves decode_verified=false. The probe never starts a task, opens another application, reads the microphone or calls the model API. Synthetic-file decode is not live voice acceptance.

References:
- https://github.com/alphacep/vosk-android-demo/blob/master/app/build.gradle
- https://github.com/alphacep/vosk-api
- https://alphacephei.com/vosk/models
- https://github.com/java-native-access/jna/tree/5.18.1

Actual physical model/decode/microphone results are recorded separately by the main debug run; this note makes no claim that those gates have passed.
