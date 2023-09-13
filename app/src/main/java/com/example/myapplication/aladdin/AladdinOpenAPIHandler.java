package com.example.myapplication.aladdin;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class AladdinOpenAPIHandler extends DefaultHandler {
    public List<Item> Items;
    private Item currentItem;
    private boolean inItemElement = false;
    private String tempValue = "";
    private boolean inDescriptionElement = false;
    private String currentTag = "";

    public AladdinOpenAPIHandler(List<Item> items) {
        Items = items;
    }

    public void startElement(String namespaceURI, String localName,String qName, Attributes atts) {
        Log.d("MyApp", "[startElement] " + localName);
        currentTag = localName;
        if (localName.equals("item")) {
            currentItem = new Item();
            inItemElement = true;
        } else if (localName.equals("title") || localName.equals("link")) {
            tempValue = "";
        } else if (localName.equals("description")) {
            tempValue = "";  // description 태그가 시작될 때마다 tempValue 초기화
            inDescriptionElement = true;  // description 태그 내부임을 표시
        }
    }

    public void characters(char[] ch,int start,int length) throws SAXException {
        String s = new String(ch,start,length);
        Log.d("MyApp", "[characters] " + s);
        if(currentTag.equals("link") || inDescriptionElement){  // description 태그 내부에서만 문자열 추가
            tempValue += s;
        } else {
            tempValue = s;
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) {
        if (inItemElement) {
            Log.d("MyApp", "[endElement] " + localName);
            if (localName.equals("item")) {
                Items.add(currentItem);
                currentItem = null;
                inItemElement = false;
            } else if (localName.equals("title")) {
                currentItem.Title = tempValue.trim();
                Log.d("MyApp","[endElement] title 값 : "+currentItem.Title);
            } else if (localName.equals("link")) {
                currentItem.Link = tempValue.trim();
                tempValue = "";
                Log.d("MyApp","[endElement] link 값 : "+currentItem.Link);
            }else if(localName.equals("description")){
                Pattern pattern = Pattern.compile("(?<=src=')[^']*");
                Matcher matcher = pattern.matcher(tempValue.trim());

                if(matcher.find()) {
                    currentItem.imageUrl=matcher.group(0);
                    Log.d("[endElement]", "Image URL: " + matcher.group(0));
                }
                inDescriptionElement=false;   // description 태그 범위 종료
            }
            currentTag = "";  // 현재 태그 종료
        }
    }
    public void parseXml(String xmlUrl) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        sp.parse(xmlUrl, this);
        Log.i("[parseXml]","XML 파싱 완료");
    }
}
