import sys
import Queue
import threading
import logging
import os
import time
from os import path

import LEP

logging.basicConfig(level=logging.DEBUG, format='(%(threadName)-10s) %(message)s')
qLock = threading.Lock()
exitFlag = False


def get_missing_bills():
    if not path.isdir(LEP.house_root):
        print('No data')
        return None

    missing_bills = {}
    try:
        with open(LEP.house_file, 'r') as f:
            for line in f:
                parts = line.split(',')
                c = int(parts[0])
                n = int(parts[1])
                missing_bills[c] = check_congress(c, n)
                print('{}: missing {} of {} bills'.format(c, len(missing_bills[c]), n))
    except:
        print('Error opening house file: {}'.format(sys.exc_info()[1]))
    return missing_bills


def check_congress(c, n):
    bills = range(1, n + 1)
    congress_root = LEP.get_congress_root_house(c)
    if not path.isdir(congress_root):
        return bills

    missing_bills = []
    for b in bills:
        bill_root = LEP.get_bill_root_house(c, b)
        if not path.isdir(bill_root):
            missing_bills.append(b)
            continue
        for t in LEP.tabs:
            bill_tab = LEP.get_bill_tab_house(c, b, t)
            if not path.exists(bill_tab):
                missing_bills.append(b)
                break
    return missing_bills


def get_house_bill_soup(c, b, t):
    url = 'https://www.congress.gov/bill/{}th-congress/house-bill/{}/{}'.format(c, b, t)
    soup = None
    tries = 0
    while soup is None and tries < 5:
        if tries > 0:
            time.sleep(tries + 1)
        soup = LEP.get_soup(url)
        tries = tries + 1
    return soup


def save_page(soup, path):
    try:
        with open(path, 'w') as f:
            f.write(soup.prettify('utf-8'))
    except:
        logging.error('error writing {}'.format(path))


def fetch_bill(c, b):
    logging.debug('start {}:{}'.format(c, b))
    # create bill directory
    root = LEP.get_bill_root_house(c, b)
    if not path.exists(root):
        os.makedirs(root)

    # fetch actions tab and header
    t = LEP.tabs[0]
    soup = get_house_bill_soup(c, b, t)
    save_page(soup.select('.featured')[0], LEP.get_bill_tab_house(c, b, 'header'))
    logging.debug('got {} {}:{}'.format('header', c, b))
    save_page(soup.select('#main')[0], LEP.get_bill_tab_house(c, b, t))
    logging.debug('got {} {}:{}'.format(t, c, b))

    # fetch other tabs
    for t in LEP.tabs[1:]:
        soup = get_house_bill_soup(c, b, t)
        eles = soup.select('#main')
        if len(eles) > 0:
            save_page(eles[0], LEP.get_bill_tab_house(c, b, t))
            logging.debug('got {} {}:{}'.format(t, c, b))
        else:
            save_page(soup, LEP.get_bill_tab_house(c, b, t))
            logging.error('missing {} {}:{}'.format(t, c, b))

    logging.debug('done {}:{}'.format(c, b))


class BillCrawler(threading.Thread):
    def __init__(self, q):
        threading.Thread.__init__(self)
        self.q = q

    def run(self):
        logging.debug('start')
        while not exitFlag:
            qLock.acquire()
            if not self.q.empty():
                c, b = self.q.get()
                qLock.release()
                fetch_bill(c, b)
                self.q.task_done()
            else:
                qLock.release()
                break
        logging.debug('stop')


def main(args):
    # set number of worker threads
    num_threads = 2
    if len(args) > 1:
        num_threads = int(args[1])

    # get map of missing bills per congress
    missing_bills = get_missing_bills()

    # flatten into queue of (congress, bill) pairs
    q = Queue.Queue()
    qLock.acquire()
    for c, bills in missing_bills.items():
        for b in bills:
            q.put((c, b))
    logging.debug('fetching {} bills'.format(q.qsize()))
    qLock.release()

    threads = []
    for i in range(num_threads):
        t = BillCrawler(q)
        t.daemon = True
        t.start()
        threads.append(t)

    q.join()

    exitFlag = True

    for t in threads:
        t.join()
    logging.debug('normal exit main thread')


if __name__ == '__main__':
    try:
        main(sys.argv)
    except KeyboardInterrupt:
        exitFlag = True
        logging.debug('early exit main thread')
        raise
