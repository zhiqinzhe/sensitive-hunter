package io.xiaochangbai.sensitive.core.main;

import io.xiaochangbai.sensitive.common.core.ICharFormat;
import io.xiaochangbai.sensitive.common.utils.StringUtil;
import io.xiaochangbai.sensitive.core.api.*;
import io.xiaochangbai.sensitive.core.config.SensitiveWordConfig;
import io.xiaochangbai.sensitive.core.support.map.SensitiveWordDefaultHandler;
import io.xiaochangbai.sensitive.core.support.replace.SensitiveWordReplaceChar;
import io.xiaochangbai.sensitive.core.support.result.WordResultHandlers;
import io.xiaochangbai.sensitive.common.utils.ArgUtil;
import io.xiaochangbai.sensitive.common.utils.CollectionUtil;
import io.xiaochangbai.sensitive.common.handler.IHandler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 敏感词引导类
 *
 * xiaochangbai
 *
 */
public class SensitiveWordDispatcher {

    private IWordHandler iWordHandler;


    private SensitiveWordConfig sensitiveWordConfig;



    private SensitiveWordDispatcher() {
    }

    private SensitiveWordDispatcher(SensitiveWordConfig config) {
        this.sensitiveWordConfig = config;
    }



    public static SensitiveWordDispatcher newInstance(SensitiveWordConfig config) {
        SensitiveWordDispatcher sensitiveWordDispatcher = new SensitiveWordDispatcher(config);
        sensitiveWordDispatcher.init();
        return sensitiveWordDispatcher;
    }

    /**
     * 初始化
     */
    public void init() {
        // 加载配置信息
        List<IWordDeny> configWordDenys = sensitiveWordConfig.getWordDenys();
        Set<String> denyList = new HashSet<>();
        if(configWordDenys!=null){
            for(IWordDeny wordDeny:configWordDenys){
                List<String> deny = wordDeny.deny();
                denyList.addAll(deny);
            }
        }
        List<IWordAllow> allowList = sensitiveWordConfig.getWordAllows();
        Set<String> allows = new HashSet<>();
        if(allowList!=null){
            for(IWordAllow wordAllow:allowList){
                List<String> deny = wordAllow.allow();
                allows.addAll(deny);
            }
        }
        List<String> results = denyList.stream().filter(e->!allows.contains(e)).collect(Collectors.toList());

        // 初始化 DFA 信息
        if(iWordHandler == null) {
            iWordHandler = new SensitiveWordDefaultHandler();
        }
        // 便于可以多次初始化
        iWordHandler.initWord(results,sensitiveWordConfig);
    }

    /**
     * 数据格式化处理
     * @param list 列表
     * @return 结果
     *1
     */
    private List<String> formatWordList(List<String> list) {
        if(CollectionUtil.isEmpty(list)) {
            return list;
        }
        List<ICharFormat> charFormats = sensitiveWordConfig.getCharFormats();
        if(charFormats==null || charFormats.size()<1){
            return list;
        }
        List<String> resultList = new ArrayList<>(list.size());
        for(String word : list) {
            if(StringUtil.isEmpty(word)) {
                continue;
            }
            StringBuilder stringBuilder = new StringBuilder();
            char[] chars = word.toCharArray();
            for(char c : chars) {
                char cf = sensitiveWordConfig.formatChar(c);
                stringBuilder.append(cf);
            }
            resultList.add(stringBuilder.toString());

        }

        return resultList;
    }


    /**
     * 是否包含敏感词
     *
     * @param target 目标字符串
     * @return 是否
     *
     */
    public boolean contains(final String target) {
        return iWordHandler.contains(target);
    }

    /**
     * 返回所有的敏感词
     * 1. 这里是默认去重的，且是有序的。
     * 2. 如果不存在，返回空列表
     *
     * @param target 目标字符串
     * @return 敏感词列表
     *
     */
    public List<String> findAll(final String target) {
        return findAll(target, WordResultHandlers.word());
    }

    /**
     * 返回第一个敏感词
     * （1）如果不存在，则返回 {@code null}
     *
     * @param target 目标字符串
     * @return 敏感词
     *
     */
    public String findFirst(final String target) {
        return findFirst(target, WordResultHandlers.word());
    }

    /**
     * 返回所有的敏感词
     * 1. 这里是默认去重的，且是有序的。
     * 2. 如果不存在，返回空列表
     *
     * @param target 目标字符串
     * @param <R> 泛型
     * @param handler 处理类
     * @return 敏感词列表
     *
     */
    public <R> List<R> findAll(final String target, final IWordResultHandler<R> handler) {
        ArgUtil.notNull(handler, "handler");
        List<IWordResult> wordResults = iWordHandler.findAll(target);
        return CollectionUtil.toList(wordResults, new IHandler<IWordResult, R>() {
            @Override
            public R handle(IWordResult wordResult) {
                return handler.handle(wordResult);
            }
        });
    }

    /**
     * 返回第一个敏感词
     * （1）如果不存在，则返回 {@code null}
     *
     * @param target 目标字符串
     * @param handler 处理类
     * @param <R> 泛型
     * @return 敏感词
     *
     */
    public <R> R findFirst(final String target, final IWordResultHandler<R> handler) {
        ArgUtil.notNull(handler, "handler");
        IWordResult wordResult = iWordHandler.findFirst(target);
        return handler.handle(wordResult);
    }


    /**
     * 替换所有内容
     *
     * @param target      目标字符串
     * @param replaceChar 替换为的 char
     * @return 替换后结果
     *
     */
    public String replace(final String target, final char replaceChar) {
        ISensitiveWordReplace replace = new SensitiveWordReplaceChar(replaceChar);

        return replace(target, replace);
    }

    /**
     * 替换所有内容
     *
     * @param target      目标字符串
     * @param replace 替换策略
     * @return 替换后结果
     *
     */
    public String replace(final String target, final ISensitiveWordReplace replace) {
        return iWordHandler.replace(target, replace);
    }

    /**
     * 替换所有内容
     * 1. 默认使用空格替换，避免星号改变 md 的格式。
     *
     * @param target 目标字符串
     * @return 替换后结果
     *
     */
    public String replace(final String target) {
        return this.replace(target, '*');
    }



}
