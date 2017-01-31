from bs4 import BeautifulSoup
from os import path
import urllib2

tabs = [
    'all-actions',
    'amendments',
    'cosponsors',
    'committees'
]
congress_file = path.relpath('.congress')
data_root = path.relpath('data')

def get_soup(url):
    req_headers = {
        "Accept-Language": "en-US,en;q=0.5",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Connection": "keep-alive",
        "Scheme": "https",
        "Authority": "congress.gov"
    }
    req = urllib2.Request(url, headers=req_headers)
    try:
        res = urllib2.urlopen(req)
    except:
        return None
    htmlDoc = res.read()
    return BeautifulSoup(htmlDoc, 'html.parser')

def get_congress_root(c):
    return path.relpath('data/{}'.format(c))

def get_bill_root(c, b):
    return path.relpath('data/{}/{}'.format(c, b)) 

def get_bill_tab(c, b, t):
    return path.relpath('data/{}/{}/{}.html'.format(c, b, t))

def get_amdt_root(c):
    return path.relpath('data/{}/amdt'.format(c))

def get_amdt_index_page(c, p):
    return path.relpath('data/{}/amdt/{}.html'.format(c, p))
