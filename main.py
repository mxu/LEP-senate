from bs4 import BeautifulSoup
import urllib
import urllib2
import re
import sys
import json
import os
import Queue
import threading
import time

exitFlag = 0

def getBillsListUrl(congress, pageSize, page):
    url = 'https://www.congress.gov/search?{}'
    q = {
        "chamber": "Senate",
        "congress": str(congress),
        "source": "legislation",
        "type": "bills"
    }
    params = {
        'q': json.dumps(q),
        'pageSort': 'documentNumber:asc',
        'pageSize': pageSize,
        'page': page
    }
    return url.format(urllib.urlencode(params))

def getBillsListSoup(congress, pageSize, page):
    return getSoup(getBillsListUrl(congress, pageSize, page))

def getNumBills(listSoup):
    resultsEle = listSoup.select('.results-number')
    if len(resultsEle) != 1:
        raise AppError('Could not find result count element')
    resultsStr = ''.join(resultsEle[0].find_all(text=True, recursive=False)).strip()
    return int(re.sub(r'[^0-9]*', '', resultsStr))

def getSoup(url):
    req = urllib2.Request(url)
    res = urllib2.urlopen(req)
    html_doc = res.read()
    return BeautifulSoup(html_doc, 'html.parser')

def getPath(congress, bill, tab):
    dataRoot = '/Users/mike.xu/code/LEP-senate/data'
    if not os.path.exists(dataRoot):
        os.makedirs(dataRoot)

    congress = dataRoot + '/{}'.format(congress)
    if not os.path.exists(congress):
        os.makedirs(congress)

    bill = congress + '/{}'.format(bill)
    if not os.path.exists(bill):
        os.makedirs(bill)

    return '{}/{}.html'.format(bill, tab)

def savePage(soup, path):
    try:
        with open(path, 'w') as f:
            f.write(soup.prettify('utf-8'))
            # print('Writing {}'.format(path))
        # return os.path.getsize(path)
    except:
        print('Error writing {}'.format(path))
        # return 0

def getSenateBillSoup(congress, bill, tab):
    url = 'https://www.congress.gov/bill/{}th-congress/senate-bill/{}/{}'.format(congress, bill, tab)
    return getSoup(url)

def getSenateBill(threadName, congress, q):
    while not exitFlag:
        queueLock.acquire()
        if not workQueue.empty():
            bill = q.get()
            queueLock.release()
            # print('Getting bill {} for congress {}'.format(bill, congress))
            tabs = [
                'all-actions',
                'amendments',
                'cosponsors',
                'committees'
            ]
            soup = getSenateBillSoup(congress, bill, tabs[0])
            savePage(soup.select('.featured')[0], getPath(congress, bill, 'header'))
            savePage(soup.select('#main')[0], getPath(congress, bill, tabs[0]))
            for tab in tabs[1:]:
                soup = getSenateBillSoup(congress, bill, tab)
                savePage(soup.select('#main')[0], getPath(congress, bill, tab))
            print('c{} b{}'.format(congress, bill))
        else:
            queueLock.release()

class BillCrawler(threading.Thread):
    def __init__(self, threadId, name, congress, q):
        threading.Thread.__init__(self)
        self.threadId = threadId
        self.name = name
        self.congress = congress
        self.q = q
    def run(self):
        getSenateBill(self.name, self.congress, self.q) 

congress = 113
limit = sys.maxint

startTime = time.time()

listSoup = getBillsListSoup(congress, 25, 1)
numBills = min(getNumBills(listSoup), limit)
print('Getting senate bills 1-{} for congress {}'.format(numBills, congress))

threadList = ['t1', 't2']
queueLock = threading.Lock()
workQueue = Queue.Queue(numBills)
threads = []
threadId = 1

for tName in threadList:
    thread = BillCrawler(threadId, tName, congress, workQueue)
    thread.start()
    threads.append(thread)
    threadId += 1

queueLock.acquire()
for bill in range(1, numBills + 1):
    workQueue.put(bill)
queueLock.release()

while not workQueue.empty():
    pass

exitFlag = 1

for t in threads:
    t.join()

elapsedTime = time.time() - startTime
print 'Exiting Main Thread ({} bills in {} seconds)'.format(numBills, elapsedTime)
