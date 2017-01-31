import sys
import os
import time
import urllib
import urllib2
import json
from os import path

import LEP

def get_amdt_index_url(c, p):
    q = {
        'chamber': 'Senate',
        'congress': str(c),
        'source': 'legislation',
        'type': 'amendments'
    }
    params = {
        'q': json.dumps(q),
        'pageSize': '250',
        'page': str(p)
    }
    url = 'https://congress.gov/search?{}'.format(urllib.urlencode(params))
    return url

def get_amdt_index_page(c, p):
    url = get_amdt_index_url(c, p)
    print(url)

    soup = None
    tries = 0
    while soup is None and tries < 5:
        if tries > 0:
            time.sleep(tries + 1)
        soup = LEP.get_soup(url)
        print('try {}'.format(tries + 1))
        tries = tries + 1
    return soup

def save_page(soup, path):
    try:
        with open(path, 'w') as f:
            f.write(soup.prettify('utf-8'))
    except:
        print('error writing {}'.format(path))

def fetch_amdt_index_page(c, p):
    soup = get_amdt_index_page(c, p)
    save_page(soup.select('.basic-search-results-lists')[0], LEP.get_amdt_index_page(c, p))
    print('got {}:{}'.format(c, p))
    next_page_link = soup.select('a.next')
    return len(next_page_link) > 0 

def fetch_amdt_index_pages(c):
    print('start {}'.format(c))
    # create amdt dir
    root = LEP.get_amdt_root(c)
    if not path.exists(root):
        os.makedirs(root)

    # fetch first page
    p = 1
    has_next = fetch_amdt_index_page(c, p)
    while has_next:
        # fetch next page if exists
        p = p + 1
        has_next = fetch_amdt_index_page(c, p)

    print('done {}'.format(c))

def main(args):
    if len(args) > 1:
        c = int(args[1])
        if len(args) > 2:
            p = int(args[2])
            fetch_amdt_index_page(c, p)
        else:
            fetch_amdt_index_pages(c)
    else:
        for c in range(97, 114):
            fetch_amdt_index_pages(c)

if __name__ == '__main__':
    main(sys.argv)
