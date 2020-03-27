/*
 *
 *
 *          Copyright (c) 2019. - TinyZ.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.struct.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.struct.annotation.StructSheet;
import org.struct.core.StructWorker;
import org.struct.core.bean.WorkerMatcher;
import org.struct.core.handler.StructHandler;
import org.struct.spi.ServiceLoader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author TinyZ.
 * @version 2019.03.23
 */
public final class WorkerUtil {

    private WorkerUtil() {
        //  no-op
    }

    public static <T> StructWorker<T> newWorker(String rootPath, Class<T> clzOfBean) {
        return newWorker(rootPath, clzOfBean, new ConcurrentHashMap<>());
    }

    public static <T> StructWorker<T> newWorker(String rootPath, Class<T> clzOfBean, Map<String, Map<Object, Object>> refFieldValueMap) {
        StructSheet annotation = AnnotationUtils.findAnnotation(StructSheet.class, clzOfBean);
        checkMissingExcelSheetAnnotation(annotation, clzOfBean);
        File file = new File(resolveFilePath(rootPath, annotation));
        List<StructHandler> collected = lookupStructHandler(annotation, file);
        for (StructHandler handler : collected) {
            try {
                return new StructWorker<>(rootPath, clzOfBean, refFieldValueMap);
            } catch (Exception e) {
                e.printStackTrace();
                //  "instance worker failure. clz:" + matcher.worker().getName(), e
//                throw new IllegalArgumentException("instance worker failure. clz:" + matcher.worker().getName(), e);
            }
        }
        throw new IllegalArgumentException("unknown data file extension. file name:" + file.getName());
    }

    public static List<StructHandler> lookupStructHandler(StructSheet annotation, File file) {
        List<StructHandler> handlers = ServiceLoader.loadAll(StructHandler.class);
        Stream<StructHandler> stream = handlers.stream();
        if (WorkerMatcher.class != annotation.matcher()) {
            stream = stream.filter(handler -> handler.matcher().getClass().isAssignableFrom(annotation.matcher()));
        } else {
            stream = stream.filter(handler -> handler.matcher().matchFile(file));
        }
        return stream.sorted(Comparator.comparingInt(o -> o.matcher().order())).collect(Collectors.toList());
    }

    public static void checkMissingExcelSheetAnnotation(StructSheet annotation, Class<?> clz) {
        if (null == annotation) {
            throw new IllegalArgumentException("class: " + clz + " undefined @StructSheet annotation");
        }
    }

    /**
     * Resolve struct file's path.
     * 1. classpath:   the path is class's path
     */
    public static String resolveFilePath(String filepath, StructSheet annotation) {
        if (filepath.startsWith("classpath:")) {
            String path = filepath.substring(filepath.indexOf(":") + 1) + "/" + annotation.fileName();
            URL resource = StructWorker.class.getResource(path);
            return resource == null
                    ? (filepath + (filepath.endsWith("/") ? "" : "/") + annotation.fileName())
                    : resource.getPath();
        } else if (filepath.startsWith("file:")) {
            return filepath.substring(filepath.indexOf(":") + 1) + "/" + annotation.fileName();
        } else {
            return filepath + "/" + annotation.fileName();
        }
    }

    private static <T, C extends Collection<T>> C newList(Class<C> clzOfList) throws Exception {
        C list;
        if (!(Collection.class.isAssignableFrom(clzOfList))) {
            throw new IllegalArgumentException("class " + clzOfList + " is not Collection.");
        }
        if (clzOfList.isInterface()
                || Modifier.isAbstract(clzOfList.getModifiers())
                || Modifier.isInterface(clzOfList.getModifiers())) {
            list = (C) new ArrayList<T>();
        } else {
            Constructor<C> constructor = clzOfList.getConstructor();
            list = constructor.newInstance();
        }
        return list;
    }

    public static <T> Collection<T> newListOnly(Class<?> clzOfList) throws Exception {
        Collection<T> list;
        if (!(Collection.class.isAssignableFrom(clzOfList))) {
            throw new IllegalArgumentException("class " + clzOfList + " is not Collection.");
        }
        if (clzOfList.isInterface()
                || Modifier.isInterface(clzOfList.getModifiers())
                || Modifier.isAbstract(clzOfList.getModifiers())) {
            list = new ArrayList<>();
        } else {
            Constructor<?> constructor = clzOfList.getConstructor();
            list = (Collection<T>) constructor.newInstance();
        }
        return list;
    }

    public static <T> Map<Object, T> newMap(Class<?> clzOfMap) throws Exception {
        Map<Object, T> map;
        if (clzOfMap == null
                || clzOfMap.isInterface()
                || Modifier.isInterface(clzOfMap.getModifiers())
                || Modifier.isAbstract(clzOfMap.getModifiers())
        ) {
            map = new HashMap<>();
        } else {
            Constructor<?> constructor = clzOfMap.getConstructor();
            map = (Map<Object, T>) constructor.newInstance();
        }
        return map;
    }

    public static Object getExcelCellValue(CellType cellType, Object cell, FormulaEvaluator evaluator) throws Exception {
        switch (cellType) {
            case _NONE:
                return null;
            case BLANK:
                return "";
            case NUMERIC:
                Double numeric;
                if (cell instanceof Cell)
                    numeric = ((Cell) cell).getNumericCellValue();
                else if (cell instanceof CellValue)
                    numeric = ((CellValue) cell).getNumberValue();
                else
                    numeric = 0.0D;
                if (numeric == numeric.longValue()) {
                    if (numeric.longValue() > Integer.MAX_VALUE) {
                        return numeric.longValue();
                    } else
                        return numeric.intValue();
                } else {
                    return numeric;
                }
            case STRING:
                if (cell instanceof Cell) {
                    return ((Cell) cell).getStringCellValue();
                } else if (cell instanceof CellValue) {
                    return ((CellValue) cell).getStringValue();
                } else {
                    return "";
                }
            case FORMULA:
                if (cell instanceof Cell) {
                    CellValue val = evaluator.evaluate((Cell) cell);
                    return getExcelCellValue(val.getCellTypeEnum(), cell, evaluator);
                } else {
                    return null;
                }
            case BOOLEAN:
                if (cell instanceof Cell)
                    return ((Cell) cell).getBooleanCellValue();
                else if (cell instanceof CellValue)
                    return ((CellValue) cell).getBooleanValue();
                else
                    return false;
            default:
                throw new Exception("Unknown Cell type");
        }
    }
}
