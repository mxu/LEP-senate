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
    req = urllib2.Request(url)
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

