from django.shortcuts import render, HttpResponse
from django.http import HttpResponseRedirect
from django.http import JsonResponse

# Create your views here.

import json
word_info = {}
all_words = []
with open(r"C:\Users\admin\Desktop\Distributed\mini_result.txt", encoding="utf8") as f:
    for line in f:
        word, info = line.split("\t")
        all_words.append(word)
        info = json.loads(info)
        word_info[word] = info
all_words.sort()

def display(request):
    response = render(request, "display.html")
    return response

def complete(request):
    def like(pattern, max_display=10):
        cnt = 0
        words = []
        for word in all_words:
            if word.startswith(pattern):
                words.append(word)
                cnt += 1
                if cnt == max_display:
                    break
        return words
    pattern = request.POST["pattern"]
    words = like(pattern)
    return JsonResponse({"words" : words})

def search(request):
    word = request.POST["word"]
    ret_info = {}
    temp_info = {}
    for key, value in word_info[word].items():
        if key != "idf":
            temp_info[key] = value
    ret_info["idf"] = word_info[word]["idf"]
    ret_info["indices"] = [temp_info]
    return JsonResponse({"word_info" : ret_info})