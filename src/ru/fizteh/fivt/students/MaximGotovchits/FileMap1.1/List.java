package filemap;

import java.util.*;

/**
 * Created by Maxim on 07.10.2014.
 */
public class List extends FileMapMain {
    void listFunction() {
        Integer size = 0;
        Set k = storage.keySet();
        for (Object iter : k) {
            if (size < storage.size() - 1) {
                System.out.print(iter + ", ");
            } else if (storage.size() != 1) {
                System.out.print(iter);
            }
            ++size;
        }
        System.out.println();
    }
}

