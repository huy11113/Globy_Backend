# File mới: embedding_service.py
from flask import Flask, request, jsonify
import google.generativeai as genai
import os

# Lấy API Key từ biến môi trường
# Chúng ta sẽ thiết lập biến này ở bước chạy
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GOOGLE_API_KEY:
    raise ValueError("Lỗi: Chưa thiết lập biến môi trường GOOGLE_API_KEY")

genai.configure(api_key=GOOGLE_API_KEY)
app = Flask(__name__)

@app.route('/embed', methods=['POST'])
def embed_text():
    try:
        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({"error": "Request phải chứa 'text'"}), 400

        text_to_embed = data['text']

        # Gọi API của Google để tạo vector
        result = genai.embed_content(
            model="models/text-embedding-004",
            content=text_to_embed,
            task_type="RETRIEVAL_QUERY"
        )

        # Trả về vector dưới dạng JSON
        return jsonify({"embedding": result['embedding']})

    except Exception as e:
        print(f"Đã xảy ra lỗi trong dịch vụ Python: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("Trợ lý AI đang lắng nghe trên cổng 5001...")
    # Chạy "trợ lý" trên cổng 5001
    app.run(port=5001)