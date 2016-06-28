import sys
from os import path

import LEP

def check_data():
    if not path.isdir(LEP.data_root):
        print('No data')
        return None

    missing_bills = {}
    try:
        with open(LEP.congress_file, 'r') as f:
            for line in f:
                parts = line.split(',')
                c = int(parts[0])
                n = int(parts[1])
                missing_bills[c] = check_congress(c, n)
                print('{}: missing {} of {} bills'.format(c, len(missing_bills[c]), n))
    except:
        print('Error opening congress file: {}'.format(sys.exc_info()[1]))
    return missing_bills

def check_congress(c, n):
    bills = range(1, n + 1)
    congress_root = LEP.get_congress_root(c)
    if not path.isdir(congress_root):
        return bills

    missing_bills = []
    for b in bills:
        bill_root = LEP.get_bill_root(c, b)
        if not path.isdir(bill_root):
            missing_bills.append(b)
            continue
        for t in LEP.tabs:
            bill_tab = LEP.get_bill_tab(c, b, t)
            if not path.exists(bill_tab):
                missing_bills.append(b)
                break
    return missing_bills 

if __name__ == '__main__':
    check_data()
