import sys
import json
import urllib
import urllib2
import re
import time

import LEP

def count_all():
    try:
        with open(LEP.house_file, 'w') as f:
            for c in range(93, 115):
                n = count_house(c)
                line = '{},{}\n'.format(c, n)
                f.write(line)
    except:
        print('Error writing house file: {}'.format(sys.exc_info()[0]))

def count_house(c):
    q = {
        'chamber': 'House',
        'congress': str(c),
        'source': 'legislation',
        'type': 'bills'
    }
    params = {
        'q': json.dumps(q),
    }
    url = 'https://www.congress.gov/search?{}'.format(urllib.urlencode(params))
    print(url)

    soup = None
    tries = 0
    while soup is None and tries < 5:
        if tries > 0:
            time.sleep(tries + 1)
        soup = LEP.get_soup(url)
        tries = tries + 1

    ele = soup.select('.results-number')
    if len(ele) < 1:
        raise AppError('Could not find result count element')
    result_str = ''.join(ele[0].find_all(text=True, recursive=False)).strip()
    return int(re.sub(r'[^0-9]*', '', result_str))

if __name__ == '__main__':
    count_all()
