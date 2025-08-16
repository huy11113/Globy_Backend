# File mới: embedding_service.py
from flask import Flask, request, jsonify
import google.generativeai as genai
import os

# Lấy API Key từ biến môi trường
# Chúng ta sẽ thiết lập biến này ở bước chạy
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
print(f"DEBUG: Khóa API được đọc từ Render là: {GOOGLE_API_KEY}") # DÒNG MỚI ĐỂ GỠ LỖI
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
    # Lấy cổng từ biến môi trường của Render, nếu không có thì mặc định là 5001
    port = int(os.environ.get('PORT', 5001))
    print(f"Trợ lý AI đang lắng nghe trên cổng {port}...")
    # Chạy server với cổng được Render cung cấp
    app.run(host='0.0.0.0', port=port)