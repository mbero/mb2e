package io.github.mbero.mb2e.infrastructure.config;

import io.github.mbero.mb2e.adapter.in.shell.BuildCommand;
import io.github.mbero.mb2e.adapter.in.shell.ExitCommand;
import io.github.mbero.mb2e.adapter.in.shell.HelpCommand;
import io.github.mbero.mb2e.adapter.in.shell.Mb2eShell;
import io.github.mbero.mb2e.adapter.in.shell.OneShotRunner;
import io.github.mbero.mb2e.adapter.in.shell.PublishCommand;
import io.github.mbero.mb2e.adapter.in.shell.RefusalReporter;
import io.github.mbero.mb2e.adapter.in.shell.RunCommand;
import io.github.mbero.mb2e.adapter.in.shell.StepLog;
import io.github.mbero.mb2e.adapter.in.shell.SyncCommand;
import io.github.mbero.mb2e.adapter.in.shell.UsageCommand;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.shell.Shell;
import org.springframework.shell.context.ShellContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.command.annotation.EnableCommand;
import org.springframework.shell.result.GenericResultHandlerService;

/**
 * Registers the mb2e shell commands and sends refusals to standard error.
 * <p>Every Spring Shell built-in command and the {@code --help} option are removed, so only
 * help, usage, exit and the mb2e commands remain; no property can bring the others back.
 */
@Configuration(proxyBeanMethods = false)
@EnableCommand({UsageCommand.class, HelpCommand.class, ExitCommand.class, BuildCommand.class, SyncCommand.class,
        PublishCommand.class, RunCommand.class})
public class ShellConfig {

    /**
     * Removes the framework's customizer that adds --help and -h to every
     * command. Its switch is a property; removing the bean is not.
     */
    @Bean
    static BeanFactoryPostProcessor noHelpOption() {
        return factory -> {
            if (factory instanceof BeanDefinitionRegistry registry
                    && registry.containsBeanDefinition("helpOptionsCommandRegistrationCustomizer")) {
                registry.removeBeanDefinition("helpOptionsCommandRegistrationCustomizer");
            }
        };
    }

    /** Replaces Spring Shell's shell with {@link Mb2eShell}, which validates every word of every line in every mode. */
    @Bean
    static BeanFactoryPostProcessor mb2eShell() {
        return factory -> {
            if (factory instanceof BeanDefinitionRegistry registry && registry.containsBeanDefinition("shell")) {
                RootBeanDefinition definition = new RootBeanDefinition(Mb2eShell.class);
                definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
                registry.removeBeanDefinition("shell");
                registry.registerBeanDefinition("shell", definition);
            }
        };
    }

    /** Runs the process arguments, each as one word; Spring Shell's own one-shot mode is disabled in application.yaml. */
    @Bean
    OneShotRunner oneShotRunner(Shell shell, ShellContext shellContext) {
        return new OneShotRunner(shell, shellContext);
    }

    /** One structured line per step on standard error, and the optional log file. */
    @Bean
    StepLog stepLog(Mb2eProperties properties) {
        return new StepLog(properties.log().file());
    }

    @Bean
    RefusalReporter.ParameterRefusals parameterRefusals() {
        return new RefusalReporter.ParameterRefusals();
    }

    /**
     * Spring Shell adds each handler to the front of a per-type list, so a handler added
     * after its own wins. Adding to the service once it exists does not depend on the
     * order of auto-configuration, as a bean-order trick would.
     */
    @Bean
    static BeanPostProcessor refusalHandlers() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof GenericResultHandlerService service) {
                    service.addResultHandler(new RefusalReporter.Failures());
                    service.addResultHandler(new RefusalReporter.UnknownCommands());
                }
                return bean;
            }
        };
    }
}
