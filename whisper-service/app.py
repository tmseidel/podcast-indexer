from flask import Flask, request, jsonify
import whisper
import tempfile
import os

app = Flask(__name__)

# Load Whisper model at startup
model_name = os.environ.get("WHISPER_MODEL", "large-v3")
print(f"Loading Whisper model: {model_name}...")
model = whisper.load_model(model_name)
print("Whisper model loaded successfully")

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
        
        # Format response
        response = {
            "text": result["text"],
            "segments": [
                {
                    "id": seg["id"],
                    "start": seg["start"],
                    "end": seg["end"],
                    "text": seg["text"]
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
