from flask import Flask, request, jsonify
import whisper
import tempfile
import os
from pyannote.audio import Pipeline

app = Flask(__name__)

# Load Whisper model at startup
print("Loading Whisper model...")
model = whisper.load_model("base")
print("Whisper model loaded successfully")

def load_diarization_pipeline():
    auth_token = os.getenv("PYANNOTE_AUTH_TOKEN")
    if not auth_token:
        print("PYANNOTE_AUTH_TOKEN not set; speaker diarization disabled")
        return None
    try:
        print("Loading Pyannote diarization pipeline...")
        pipeline = Pipeline.from_pretrained("pyannote/speaker-diarization-3.1", token=auth_token)
        print("Pyannote diarization pipeline loaded successfully")
        return pipeline
    except Exception as exc:
        print(f"Failed to load diarization pipeline: {exc}")
        return None

diarization_pipeline = None
diarization_pipeline_loaded = False

def get_diarization_pipeline():
    global diarization_pipeline, diarization_pipeline_loaded
    if diarization_pipeline_loaded:
        return diarization_pipeline
    diarization_pipeline_loaded = True
    diarization_pipeline = load_diarization_pipeline()
    return diarization_pipeline

def build_speaker_segments(audio_path):
    pipeline = get_diarization_pipeline()
    if pipeline is None:
        return []
    try:
        diarization = pipeline(audio_path)
        speakers = []
        for turn, _, speaker in diarization.itertracks(yield_label=True):
            speakers.append({
                "start": turn.start,
                "end": turn.end,
                "speaker": speaker
            })
        return speakers
    except Exception as exc:
        print(f"Speaker diarization failed: {exc}")
        return []

def normalize_speaker_label(raw_label):
    if not raw_label:
        return None
    if raw_label.strip().lower().startswith("speaker"):
        return raw_label.replace("_", " ")
    return f"Speaker {raw_label}".replace("_", " ")

def assign_speaker_to_segment(segment, speaker_segments):
    if not speaker_segments:
        return None
    seg_start = segment["start"]
    seg_end = segment["end"]
    best_label = None
    best_overlap = 0.0
    for speaker_segment in speaker_segments:
        overlap = min(seg_end, speaker_segment["end"]) - max(seg_start, speaker_segment["start"])
        if overlap <= 0:
            continue
        if overlap > best_overlap:
            best_overlap = overlap
            best_label = speaker_segment["speaker"]
    return normalize_speaker_label(best_label) if best_overlap > 0 else None

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"})

@app.route('/', methods=['GET'])
def root():
    return health()

@app.route('/transcribe', methods=['POST'])
def transcribe():
    if 'file' not in request.files:
        return jsonify({"error": "No file provided"}), 400
    
    audio_file = request.files['file']
    
    # Save to temporary file
    with tempfile.NamedTemporaryFile(delete=False, suffix='.mp3') as tmp_file:
        audio_file.save(tmp_file.name)
        tmp_path = tmp_file.name
    
    try:
        # Transcribe
        result = model.transcribe(tmp_path, verbose=False)
        speaker_segments = build_speaker_segments(tmp_path)
        
        # Format response
        response = {
            "text": result["text"],
            "segments": [
                {
                    "id": seg["id"],
                    "start": seg["start"],
                    "end": seg["end"],
                    "text": seg["text"],
                    "speaker": assign_speaker_to_segment(seg, speaker_segments)
                }
                for seg in result["segments"]
            ]
        }
        
        return jsonify(response)
    finally:
        # Clean up temp file
        if os.path.exists(tmp_path):
            os.remove(tmp_path)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
