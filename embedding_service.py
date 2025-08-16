# File: embedding_service.py
from flask import Flask, request, jsonify
import requests
import os
import json

# Lấy API Key từ biến môi trường
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GOOGLE_API_KEY:
    raise ValueError("Lỗi: Chưa thiết lập biến môi trường GOOGLE_API_KEY")

app = Flask(__name__)

# URL Endpoint của Google Gemini cho embedding
GEMINI_EMBEDDING_URL = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent"

@app.route('/embed', methods=['POST', 'GET']) # Chấp nhận cả phương thức GET
def embed_text():
    try:
        # Thay đổi logic để đọc từ query parameter nếu là GET request
        if request.method == 'GET':
            text_to_embed = request.args.get('text')
            if not text_to_embed:
                return jsonify({"error": "Request GET phải chứa tham số 'text'"}), 400
        else: # Xử lý POST request như bình thường
            data = request.get_json()
            if not data or 'text' not in data:
                return jsonify({"error": "Request POST phải chứa 'text'"}), 400
            text_to_embed = data['text']

        # Dữ liệu payload cho API
        payload = {
            "model": "models/text-embedding-004",
            "content": {
                "parts": [
                    {"text": text_to_embed}
                ]
            }
        }

        # Thêm task_type chỉ khi nó có giá trị
        if "RETRIEVAL_QUERY":
            payload["task_type"] = "RETRIEVAL_QUERY"

        # Header yêu cầu, bao gồm API Key
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": GOOGLE_API_KEY
        }

        # Gửi yêu cầu HTTP POST trực tiếp đến API của Google
        response = requests.post(GEMINI_EMBEDDING_URL, headers=headers, data=json.dumps(payload))
        response.raise_for_status() # Ném lỗi nếu mã trạng thái không phải 2xx

        # Xử lý kết quả trả về
        result = response.json()
        embedding = result['embedding']
        return jsonify({"embedding": embedding})

    except requests.exceptions.RequestException as e:
        print(f"LỖI KẾT NỐI VỚI GOOGLE API: {e}")
        return jsonify({"error": "Không thể kết nối với dịch vụ Google AI. " + str(e)}), 500
    except Exception as e:
        print(f"Đã xảy ra lỗi trong dịch vụ Python: {e}")
        return jsonify({"error": "Lỗi nội bộ. " + str(e)}), 500

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5001))
    print(f"Trợ lý AI đang lắng nghe trên cổng {port}...")
    app.run(host='0.0.0.0', port=port)